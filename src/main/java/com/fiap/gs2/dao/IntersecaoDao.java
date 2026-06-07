package com.fiap.gs2.dao;

import com.fiap.gs2.dto.IntersecaoDto;
import com.fiap.gs2.util.DataUtil;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IntersecaoDao extends DataUtil {

    private static final String SQL_MATERIALIZAR = """
            INSERT INTO AREA_INTERSECAO (
                id_area_desmatamento, id_area_protegida,
                codigo_desmatamento, codigo_conservacao,
                nome_conservacao, categoria, municipio, uf,
                area_intersecao_ha, geojson_intersecao
            )
            SELECT
                t.id_area_desmatamento,
                t.id_area_protegida,
                t.codigo_desmatamento,
                t.codigo_conservacao,
                t.nome,
                t.categoria,
                t.municipio,
                t.uf,
                ROUND(SDO_GEOM.SDO_AREA(SDO_CS.TRANSFORM(t.intersecao_geom, 5880), 0.005) / 10000, 4),
                SDO_UTIL.TO_GEOJSON(SDO_CS.TRANSFORM(t.intersecao_geom, 4326))
            FROM (
                SELECT
                    d.id_area_desmatamento,
                    c.id_area_protegida,
                    d.codigo_externo AS codigo_desmatamento,
                    c.codigo_externo AS codigo_conservacao,
                    d.municipio,
                    d.uf,
                    c.nome,
                    c.categoria,
                    SDO_GEOM.SDO_INTERSECTION(d.geometria, c.geometria, 0.005) AS intersecao_geom
                FROM AREA_DESMATAMENTO d
                JOIN AREA_PROTEGIDA c
                    ON SDO_ANYINTERACT(d.geometria, c.geometria) = 'TRUE'
                WHERE d.ativo = 'S'
                  AND c.ativa = 'S'
            ) t
            WHERE t.intersecao_geom IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM AREA_INTERSECAO ai
                  WHERE ai.id_area_desmatamento = t.id_area_desmatamento
                    AND ai.id_area_protegida   = t.id_area_protegida
              )
            """;

    private static final String SQL_LISTAR = """
            SELECT id_intersecao, codigo_desmatamento, codigo_conservacao,
                   nome_conservacao, categoria, municipio, uf,
                   area_intersecao_ha, geojson_intersecao
              FROM AREA_INTERSECAO
             ORDER BY area_intersecao_ha DESC
            """;

    /**
     * Roda o cruzamento espacial e grava os pares novos. Retorna quantos foram
     * inseridos.
     */
    public int materializar() {
        return jdbcTemplate.update(SQL_MATERIALIZAR);
    }

    /**
     * Lê o dado já pré-processado para o front.
     */
    public List<IntersecaoDto> listar() {
        return jdbcTemplate.query(SQL_LISTAR, (rs, n) -> new IntersecaoDto(
                rs.getLong("id_intersecao"),
                rs.getString("codigo_desmatamento"),
                rs.getString("codigo_conservacao"),
                rs.getString("nome_conservacao"),
                rs.getString("categoria"),
                rs.getString("municipio"),
                rs.getString("uf"),
                rs.getBigDecimal("area_intersecao_ha"),
                rs.getString("geojson_intersecao")));
    }
}
