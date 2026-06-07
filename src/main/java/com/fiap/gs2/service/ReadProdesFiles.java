package com.fiap.gs2.service;

import com.fiap.gs2.dao.DadoOrigemDao;
import com.fiap.gs2.dao.DeforestationAreaDao;
import com.fiap.gs2.model.DeforestationArea;
import com.fiap.gs2.util.DataUtil;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.CRC32;

@Service
public class ReadProdesFiles extends DataUtil {

    private final DeforestationAreaDao deforestationAreaDao;
    private final DadoOrigemDao dadoOrigemDao;
    private final Path inputDirectory;
    private final boolean importOnStartup;

    public ReadProdesFiles(
            DeforestationAreaDao deforestationAreaDao,
            DadoOrigemDao dadoOrigemDao,
            @Value("${prodes.input-directory:input/areas_desmatamento}") String inputDirectory,
            @Value("${prodes.import-on-startup:true}") boolean importOnStartup) {
        this.deforestationAreaDao = deforestationAreaDao;
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
            throw new IllegalStateException("Diretorio de entrada PRODES nao encontrado: " + inputDirectory.toAbsolutePath());
        }

        try (var geopackages = Files.walk(inputDirectory)) {
            var files = geopackages
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gpkg"))
                    .sorted()
                    .toList();

            for (Path geopackage : files) {
                importGeoPackage(geopackage);
            }
        }
    }

    private void importGeoPackage(Path geopackage) throws IOException {
        String nomeArquivo = inputDirectory.relativize(geopackage).toString().replace('\\', '/');
        long hashArquivo = calculateHash(geopackage);
        long idArquivo = dadoOrigemDao.upsertDadoOrigem(nomeArquivo, hashArquivo, TIPO_CONTEUDO_DESMATAMENTO);

        try {
            deforestationAreaDao.deleteAreaDesmatamento(idArquivo);
            importFeatures(geopackage, idArquivo);
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

    private void importFeatures(Path geopackagePath, long idArquivo) throws IOException {
        GeoPackage geoPackage = new GeoPackage(geopackagePath.toFile());
        try {
            for (FeatureEntry entry : geoPackage.features()) {
                importFeatureEntry(geoPackage, entry, idArquivo);
            }
        } finally {
            geoPackage.close();
        }
    }

    private void importFeatureEntry(GeoPackage geoPackage, FeatureEntry entry, long idArquivo) throws IOException {
        try (SimpleFeatureReader reader = geoPackage.reader(entry, null, null)) {
            SimpleFeatureType schema = reader.getFeatureType();
            MathTransform storageTransform = createTransform(schema, DATA_CRS);
            MathTransform areaTransform = createTransform(schema, AREA_CRS);

            while (reader.hasNext()) {
                SimpleFeature feature = reader.next();
                DeforestationArea area = DeforestationArea.from(feature, schema, storageTransform, areaTransform);
                deforestationAreaDao.insertAreaDesmatamento(idArquivo, area);
            }
        }
    }

    private static long calculateHash(Path file) throws IOException {
        CRC32 crc32 = new CRC32();
        byte[] bytes = Files.readAllBytes(file);
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    private static MathTransform createTransform(SimpleFeatureType schema, CoordinateReferenceSystem targetCrs) {
        try {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            CoordinateReferenceSystem sourceCrs = geometryDescriptor == null
                    ? null
                    : geometryDescriptor.getCoordinateReferenceSystem();
            if (sourceCrs == null) {
                sourceCrs = DATA_CRS;
            }
            return CRS.findMathTransform(sourceCrs, targetCrs, true);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel preparar transformacao de geometria", ex);
        }
    }
}
