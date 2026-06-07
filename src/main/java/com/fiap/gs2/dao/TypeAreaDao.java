package com.fiap.gs2.dao;

import com.fiap.gs2.util.DataUtil;
import org.springframework.stereotype.Repository;

@Repository
public class TypeAreaDao extends DataUtil {
    public long findOrCreateTipoArea(String nomeTipo) {
        Long existingId = jdbcTemplate.query(
                "SELECT id_tipo_area FROM TIPO_AREA_PROTEGIDA WHERE nome = ?",
                rs -> rs.next() ? rs.getLong("id_tipo_area") : null,
                nomeTipo);

        if (existingId != null) {
            return existingId;
        }

        long idTipoArea = nextId("TIPO_AREA_PROTEGIDA", "id_tipo_area");
        jdbcTemplate.update(
                """
                INSERT INTO TIPO_AREA_PROTEGIDA
                    (id_tipo_area, nome, descricao, restrita)
                VALUES
                    (?, ?, ?, ?)
                """,
                idTipoArea,
                nomeTipo,
                "Tipo criado automaticamente durante leitura de Shapefile",
                AREA_RESTRITA);
        return idTipoArea;
    }
}
