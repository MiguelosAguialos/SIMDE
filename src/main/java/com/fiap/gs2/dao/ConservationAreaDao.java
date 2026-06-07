package com.fiap.gs2.dao;

import com.fiap.gs2.model.ConservationArea;
import com.fiap.gs2.util.DataUtil;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

@Repository
public class ConservationAreaDao extends DataUtil {
    public void insertAreaProtegida(long idArquivo, long idTipoArea, ConservationArea area) {
        long idAreaProtegida = nextId("AREA_PROTEGIDA", "id_area_protegida");
        jdbcTemplate.execute(
                """
                INSERT INTO AREA_PROTEGIDA
                    (id_area_protegida, id_arquivo, id_tipo_area, codigo_externo, nome, categoria,
                     orgao_responsavel, uf, area_ha, geometria, ativa, criado_em, atualizado_em)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?, ?, ?,
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
                    ps.setLong(1, idAreaProtegida);
                    ps.setLong(2, idArquivo);
                    ps.setLong(3, idTipoArea);
                    setNullableString(ps, 4, area.codigoExterno());
                    setNullableString(ps, 5, area.nome());
                    setNullableString(ps, 6, area.categoria());
                    setNullableString(ps, 7, area.orgaoResponsavel());
                    setNullableString(ps, 8, area.uf());
                    setNullableBigDecimal(ps, 9, area.areaHa());
                    setClob(ps, 10, area.wkt());
                    ps.setString(11, AREA_PROTEGIDA_ATIVA);
                    ps.executeUpdate();
                    return null;
                });
    }

    public void deleteAreaProtegida(long idArquivo) {
        jdbcTemplate.update("DELETE FROM AREA_PROTEGIDA WHERE id_arquivo = ?", idArquivo);
    }
}
