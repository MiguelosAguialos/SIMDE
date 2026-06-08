package com.fiap.gs2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.gs2.dao.IntersecaoDao;
import com.fiap.gs2.dto.IntersecaoDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntersecaoControllerTest {

    @Test
    void listarAcceptsOracleGeoJsonWithLeadingDecimalPoint() throws Exception {
        IntersecaoDao dao = mock(IntersecaoDao.class);
        when(dao.listar()).thenReturn(List.of(new IntersecaoDto(
                1L,
                "DESM-1",
                "CONS-1",
                "Area protegida",
                "UC",
                "Municipio",
                "PA",
                BigDecimal.ONE,
                """
                        { "type": "Polygon", "coordinates": [ [ [-47.7398589629182, -.769848619530646] ] ] }
                        """)));

        Map<String, Object> response = new IntersecaoController(dao).listar();

        assertEquals("FeatureCollection", response.get("type"));

        List<?> features = assertInstanceOf(List.class, response.get("features"));
        Map<?, ?> feature = assertInstanceOf(Map.class, features.getFirst());
        JsonNode geometry = assertInstanceOf(JsonNode.class, feature.get("geometry"));

        assertEquals("Polygon", geometry.get("type").asText());
        assertEquals(-0.769848619530646, geometry.get("coordinates").get(0).get(0).get(1).asDouble());
    }
}
