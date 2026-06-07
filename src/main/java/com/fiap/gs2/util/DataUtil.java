package com.fiap.gs2.util;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

@Component
public class DataUtil {

    public static final String TIPO_CONTEUDO = "AREA_PROTEGIDA";
    public static final String STATUS_PROCESSADO = "PROCESSADO";
    public static final String STATUS_ERRO = "ERRO";
    public static final String AREA_PROTEGIDA_ATIVA = "S";
    public static final String AREA_RESTRITA = "S";
    public static final CoordinateReferenceSystem AREA_CRS = decodeAreaCrs();

    @Autowired
    public JdbcTemplate jdbcTemplate;

    public long nextId(String tableName, String columnName) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "SELECT NVL(MAX(" + columnName + "), 0) + 1 FROM " + tableName,
                Long.class));
    }

    public static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    public static void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    public static void setClob(PreparedStatement ps, int index, String value) throws SQLException {
        Reader reader = new StringReader(value);
        ps.setClob(index, reader, value.length());
    }

    private static CoordinateReferenceSystem decodeAreaCrs() {
        try {
            return CRS.decode("EPSG:5880", true);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel carregar EPSG:5880 para calculo de area", ex);
        }
    }
}
