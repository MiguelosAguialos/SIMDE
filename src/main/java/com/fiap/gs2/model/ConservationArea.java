package com.fiap.gs2.model;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;

import java.math.BigDecimal;

public record ConservationArea(
        String codigoExterno,
        String nome,
        String categoria,
        String tipoArea,
        String orgaoResponsavel,
        String uf,
        BigDecimal areaHa,
        String wkt) {

    public static final WKTWriter WKT_WRITER = new WKTWriter();

    public static ConservationArea from(SimpleFeature feature, SimpleFeatureType schema, MathTransform areaTransform) {
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalStateException("Feature sem geometria: " + feature.getID());
        }

        geometry.setSRID(4674);
        Geometry areaGeometry = transformForArea(geometry, areaTransform);
        BigDecimal areaHa = BigDecimal.valueOf(areaGeometry.getArea() / 10_000D);

        String codigoExterno = firstText(feature, schema, "id", "terrai_cod", "codigo", "cod");
        String nome = firstText(feature, schema, "nome", "terrai_nom", "name");
        String categoria = firstText(feature, schema, "categoria", "fase_ti", "grupo");
        String tipoArea = resolveTipoArea(feature, schema);
        String orgaoResponsavel = firstText(feature, schema, "orgao_responsavel", "orgao", "esfera");
        String uf = firstText(feature, schema, "uf", "sigla_uf");

        return new ConservationArea(
                codigoExterno,
                nome,
                categoria,
                tipoArea,
                orgaoResponsavel,
                uf,
                areaHa,
                WKT_WRITER.write(geometry));
    }

    private static Geometry transformForArea(Geometry geometry, MathTransform areaTransform) {
        try {
            return JTS.transform(geometry, areaTransform);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel calcular area da geometria", ex);
        }
    }

    private static String resolveTipoArea(SimpleFeature feature, SimpleFeatureType schema) {
        if (hasAttribute(schema, "terrai_nom") || hasAttribute(schema, "fase_ti")) {
            return "TERRA_INDIGENA";
        }

        String categoria = firstText(feature, schema, "categoria", "grupo");
        if (categoria != null && !categoria.isBlank()) {
            return categoria;
        }

        return "UNIDADE_CONSERVACAO";
    }

    private static String firstText(SimpleFeature feature, SimpleFeatureType schema, String... names) {
        for (String name : names) {
            if (hasAttribute(schema, name)) {
                Object value = feature.getAttribute(name);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString().trim();
                }
            }
        }
        return null;
    }

    private static boolean hasAttribute(SimpleFeatureType schema, String name) {
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor.getLocalName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
