package com.fiap.gs2.dto;

import java.math.BigDecimal;

public record IntersecaoDto(
        Long id,
        String codigoDesmatamento,
        String codigoConservacao,
        String nomeConservacao,
        String categoria,
        String municipio,
        String uf,
        BigDecimal areaIntersecaoHa,
        String geojson) {

}
