package com.fiap.gs2.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.CRC32;

import com.fiap.gs2.dao.ConservationAreaDao;
import com.fiap.gs2.dao.DadoOrigemDao;
import com.fiap.gs2.dao.TypeAreaDao;
import com.fiap.gs2.model.ConservationArea;
import com.fiap.gs2.util.DataUtil;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadConservationAreaFiles extends DataUtil {

    private final ConservationAreaDao conservationAreaDao;
    private final TypeAreaDao typeAreaDao;
    private final DadoOrigemDao dadoOrigemDao;
    private final Path inputDirectory;
    private final boolean importOnStartup;

    public ReadConservationAreaFiles(
            ConservationAreaDao conservationAreaDao,
            TypeAreaDao typeAreaDao,
            DadoOrigemDao dadoOrigemDao,
            @Value("${input.directory:input/areas_conservacao}") String inputDirectory,
            @Value("${conservation-area.import-on-startup:false}") boolean importOnStartup) {
        this.conservationAreaDao = conservationAreaDao;
        this.typeAreaDao = typeAreaDao;
        this.dadoOrigemDao = dadoOrigemDao;
        this.inputDirectory = Path.of(inputDirectory);
        this.importOnStartup = importOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importFilesWhenApplicationStarts() throws IOException {
        if (importOnStartup) {
            readAllFiles();
        }
    }

    @Transactional
    public void readAllFiles() throws IOException {
        if (!Files.isDirectory(inputDirectory)) {
            throw new IllegalStateException("Diretorio de entrada nao encontrado: " + inputDirectory.toAbsolutePath());
        }

        try (var shapefiles = Files.walk(inputDirectory)) {
            var files = shapefiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".shp"))
                    .sorted()
                    .toList();

            for (Path shapefile : files) {
                importShapefile(shapefile);
            }
        }
    }

    private void importShapefile(Path shapefile) throws IOException {
        var nomeArquivo = inputDirectory.relativize(shapefile).toString().replace('\\', '/');
        var hashArquivo = calculateHash(shapefile);
        var idArquivo = dadoOrigemDao.upsertDadoOrigem(nomeArquivo, hashArquivo);

        try {
            conservationAreaDao.deleteAreaProtegida(idArquivo);
            importFeatures(shapefile, idArquivo);
            jdbcTemplate.update(
                    "UPDATE DADO_ORIGEM SET status_processamento = ?, processado_em = SYSTIMESTAMP WHERE id_arquivo = ?",
                    STATUS_PROCESSADO,
                    idArquivo);
        } catch (RuntimeException | IOException ex) {
            jdbcTemplate.update(
                    "UPDATE DADO_ORIGEM SET status_processamento = ?, processado_em = SYSTIMESTAMP WHERE id_arquivo = ?",
                    STATUS_ERRO,
                    idArquivo);
            throw ex;
        }
    }

    private void importFeatures(Path shapefile, long idArquivo) throws IOException {
        FileDataStore dataStore = FileDataStoreFinder.getDataStore(shapefile.toUri().toURL());
        if (dataStore == null) {
            throw new IllegalStateException("Nao foi possivel abrir o Shapefile: " + shapefile);
        }

        try {
            SimpleFeatureSource featureSource = dataStore.getFeatureSource();
            SimpleFeatureType schema = featureSource.getSchema();
            MathTransform areaTransform = createAreaTransform(schema);
            SimpleFeatureCollection collection = featureSource.getFeatures();

            try (SimpleFeatureIterator iterator = collection.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    var area = ConservationArea.from(feature, schema, areaTransform);
                    var idTipoArea = typeAreaDao.findOrCreateTipoArea(area.tipoArea());
                    conservationAreaDao.insertAreaProtegida(idArquivo, idTipoArea, area);
                }
            }
        } finally {
            dataStore.dispose();
        }
    }

    private static long calculateHash(Path shapefile) throws IOException {
        CRC32 crc32 = new CRC32();
        Path directory = shapefile.getParent();
        String baseName = fileNameWithoutExtension(shapefile);

        try (var files = Files.list(directory)) {
            for (Path file : files
                    .filter(Files::isRegularFile)
                    .filter(path -> fileNameWithoutExtension(path).equals(baseName))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList()) {
                byte[] bytes = Files.readAllBytes(file);
                crc32.update(bytes, 0, bytes.length);
            }
        }

        return crc32.getValue();
    }

    private static String fileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private static MathTransform createAreaTransform(SimpleFeatureType schema) {
        try {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            CoordinateReferenceSystem sourceCrs = geometryDescriptor == null
                    ? null
                    : geometryDescriptor.getCoordinateReferenceSystem();
            if (sourceCrs == null) {
                sourceCrs = CRS.decode("EPSG:4674", true);
            }
            return CRS.findMathTransform(sourceCrs, AREA_CRS, true);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel preparar transformacao para calculo de area", ex);
        }
    }
}
