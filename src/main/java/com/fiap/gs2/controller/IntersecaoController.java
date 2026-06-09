package com.fiap.gs2.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.gs2.dao.IntersecaoDao;
import com.fiap.gs2.dto.IntersecaoDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intersecoes")
@CrossOrigin(origins = "*") // dev: liberar o front local; restringir depois
public class IntersecaoController {

    private final IntersecaoDao intersecaoDao;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS);

    public IntersecaoController(IntersecaoDao intersecaoDao) {
        this.intersecaoDao = intersecaoDao;
    }

    /**
     * Devolve as intersecoes como GeoJSON FeatureCollection (pronto para
     * L.geoJSON no Leaflet). Cada feature traz a geometria da intersecao (em
     * 4326) e os atributos em properties.
     */
    @GetMapping
    public Map<String, Object> listar() throws Exception {
        List<IntersecaoDto> intersecoes = intersecaoDao.listar();
        List<Map<String, Object>> features = new ArrayList<>();

        for (IntersecaoDto i : intersecoes) {
            Map<String, Object> feature = new LinkedHashMap<>();
            JsonNode geojson = objectMapper.readTree(i.geojson());
            feature.put("type", geojson.get("type").asText());
            feature.put("coordinates", i.geojson() == null ? null : objectMapper.readValue(geojson.get("coordinates").toString(), Object.class));
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", i.id());
            props.put("codigoDesmatamento", i.codigoDesmatamento());
            props.put("codigoConservacao", i.codigoConservacao());
            props.put("nomeConservacao", i.nomeConservacao());
            props.put("categoria", i.categoria());
            props.put("municipio", i.municipio());
            props.put("uf", i.uf());
            props.put("areaIntersecaoHa", i.areaIntersecaoHa());
            feature.put("properties", props);

            features.add(feature);
        }

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    /**
     * Recalcula as intersecoes sob demanda (grava só os pares novos).
     */
    @PostMapping("/recalcular")
    public Map<String, Object> recalcular() {
        int novas = intersecaoDao.materializar();
        return Map.of("novasIntersecoes", novas);
    }
}
