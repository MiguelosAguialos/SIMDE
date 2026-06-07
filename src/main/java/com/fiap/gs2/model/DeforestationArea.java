package com.fiap.gs2.model;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public record DeforestationArea(
        String codigoExterno,
        LocalDate dataDeteccao,
        LocalDate dataImagem,
        String fonteSatelite,
        String bioma,
        String municipio,
        String uf,
        BigDecimal areaHa,
        String wkt) {

    private static final WKTWriter WKT_WRITER = new WKTWriter();
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static DeforestationArea from(
            SimpleFeature feature,
            SimpleFeatureType schema,
            MathTransform storageTransform,
            MathTransform areaTransform) {
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalStateException("Feature sem geometria: " + feature.getID());
        }

        Geometry storageGeometry = transform(geometry, storageTransform, "armazenar geometria");
        storageGeometry.setSRID(4674);
        Geometry areaGeometry = transform(geometry, areaTransform, "calcular area");

        BigDecimal areaHa = firstDecimal(feature, schema, "area_ha", "areaha", "area", "hectares", "ha");
        if (areaHa == null) {
            BigDecimal areaKm2 = firstDecimal(feature, schema, "area_km", "area_km2", "areakm2", "km2");
            areaHa = areaKm2 == null
                    ? BigDecimal.valueOf(areaGeometry.getArea() / 10_000D)
                    : areaKm2.multiply(BigDecimal.valueOf(100));
        }

        return new DeforestationArea(
                firstText(feature, schema, "codigo_externo", "id", "fid", "uuid", "gid", "objectid"),
                firstDate(feature, schema, "data_deteccao", "data_detect", "dt_deteccao", "detect_date", "data"),
                firstDate(feature, schema, "data_imagem", "dt_imagem", "image_date", "data_img", "view_date"),
                firstText(feature, schema, "fonte_satelite", "satelite", "satellite", "sensor", "fonte"),
                firstText(feature, schema, "bioma", "biome"),
                firstText(feature, schema, "municipio", "municipio_nome", "mun_name", "nm_mun", "county"),
                firstText(feature, schema, "uf", "estado", "state", "sigla_uf"),
                areaHa,
                WKT_WRITER.write(storageGeometry));
    }

    private static Geometry transform(Geometry geometry, MathTransform transform, String action) {
        try {
            return JTS.transform(geometry, transform);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel " + action, ex);
        }
    }

    private static String firstText(SimpleFeature feature, SimpleFeatureType schema, String... names) {
        for (String name : names) {
            String attributeName = attributeName(schema, name);
            if (attributeName != null) {
                Object value = feature.getAttribute(attributeName);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString().trim();
                }
            }
        }
        return null;
    }

    private static LocalDate firstDate(SimpleFeature feature, SimpleFeatureType schema, String... names) {
        for (String name : names) {
            String attributeName = attributeName(schema, name);
            if (attributeName != null) {
                LocalDate date = parseDate(feature.getAttribute(attributeName));
                if (date != null) {
                    return date;
                }
            }
        }
        return null;
    }

    private static BigDecimal firstDecimal(SimpleFeature feature, SimpleFeatureType schema, String... names) {
        for (String name : names) {
            String attributeName = attributeName(schema, name);
            if (attributeName != null) {
                BigDecimal decimal = parseDecimal(feature.getAttribute(attributeName));
                if (decimal != null) {
                    return decimal;
                }
            }
        }
        return null;
    }

    private static LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }

        String normalized = text.contains("T") ? text.substring(0, text.indexOf('T')) : text;
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(normalized, BRAZILIAN_DATE);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private static BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String attributeName(SimpleFeatureType schema, String name) {
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor.getLocalName().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) {
                return descriptor.getLocalName();
            }
        }
        return null;
    }
}
