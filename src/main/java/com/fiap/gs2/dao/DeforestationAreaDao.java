package com.fiap.gs2.dao;

import com.fiap.gs2.model.DeforestationArea;
import com.fiap.gs2.util.DataUtil;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

@Repository
public class DeforestationAreaDao extends DataUtil {
    public void insertAreaDesmatamento(long idArquivo, DeforestationArea area) {
        long idAreaDesmatamento = nextId("AREA_DESMATAMENTO", "id_area_desmatamento");
        jdbcTemplate.execute(
                """
                INSERT INTO AREA_DESMATAMENTO
                    (id_area_desmatamento, id_arquivo, codigo_externo, data_deteccao, data_imagem,
                     fonte_satelite, bioma, municipio, uf, area_ha, geometria, ativo, criado_em, atualizado_em)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                     (SELECT MDSYS.SDO_GEOMETRY(
                                 wkt_geom.g.SDO_GTYPE,
                                 4674,
                                 wkt_geom.g.SDO_POINT,
                                 wkt_geom.g.SDO_ELEM_INFO,
                                 wkt_geom.g.SDO_ORDINATES)
                        FROM (SELECT SDO_UTIL.FROM_WKTGEOMETRY(?) g FROM DUAL) wkt_geom),
                     ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """,
                (PreparedStatementCallback<Void>) ps -> {
                    ps.setLong(1, idAreaDesmatamento);
                    ps.setLong(2, idArquivo);
                    setNullableString(ps, 3, area.codigoExterno());
                    setNullableDate(ps, 4, area.dataDeteccao());
                    setNullableDate(ps, 5, area.dataImagem());
                    setNullableString(ps, 6, area.fonteSatelite());
                    setNullableString(ps, 7, area.bioma());
                    setNullableString(ps, 8, area.municipio());
                    setNullableString(ps, 9, area.uf());
                    setNullableBigDecimal(ps, 10, area.areaHa());
                    setClob(ps, 11, area.wkt());
                    ps.setString(12, AREA_DESMATAMENTO_ATIVO);
                    ps.executeUpdate();
                    return null;
                });
    }

    public void deleteAreaDesmatamento(long idArquivo) {
        jdbcTemplate.update("DELETE FROM AREA_DESMATAMENTO WHERE id_arquivo = ?", idArquivo);
    }
}
