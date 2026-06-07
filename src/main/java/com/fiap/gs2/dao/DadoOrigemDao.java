package com.fiap.gs2.dao;

import com.fiap.gs2.util.DataUtil;
import org.springframework.stereotype.Repository;

@Repository
public class DadoOrigemDao extends DataUtil {
    public long upsertDadoOrigem(String nomeArquivo, long hashArquivo) {
        Long existingId = jdbcTemplate.query(
                "SELECT id_arquivo FROM DADO_ORIGEM WHERE nome_arquivo = ?",
                rs -> rs.next() ? rs.getLong("id_arquivo") : null,
                nomeArquivo);

        if (existingId != null) {
            jdbcTemplate.update(
                    """
                    UPDATE DADO_ORIGEM
                       SET tipo_conteudo = ?,
                           hash_arquivo = ?,
                           status_processamento = ?,
                           processado_em = SYSTIMESTAMP
                     WHERE id_arquivo = ?
                    """,
                    TIPO_CONTEUDO,
                    hashArquivo,
                    "PROCESSANDO",
                    existingId);
            return existingId;
        }

        long idArquivo = nextId("DADO_ORIGEM", "id_arquivo");
        jdbcTemplate.update(
                """
                INSERT INTO DADO_ORIGEM
                    (id_arquivo, nome_arquivo, tipo_conteudo, hash_arquivo, status_processamento, processado_em)
                VALUES
                    (?, ?, ?, ?, ?, SYSTIMESTAMP)
                """,
                idArquivo,
                nomeArquivo,
                TIPO_CONTEUDO,
                hashArquivo,
                "PROCESSANDO");
        return idArquivo;
    }
}
