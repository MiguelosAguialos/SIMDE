import { MapContainer, TileLayer, GeoJSON, CircleMarker } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect, useMemo, useRef } from 'react';

interface IntersecaoFeature {
  type: 'Feature';
  geometry: any;
  properties: {
    id: number;
    codigoDesmatamento: string;
    codigoConservacao: string;
    nomeConservacao: string;
    categoria: string;
    municipio: string;
    uf: string;
    areaIntersecaoHa: number;
  };
}

interface MapViewProps {
  data: IntersecaoFeature[];
  selectedFeature?: IntersecaoFeature | null;
  onFeatureSelect?: (feature: IntersecaoFeature) => void;
}

export default function MapView({ data, selectedFeature, onFeatureSelect }: MapViewProps) {
  const mapRef = useRef<L.Map | null>(null);
  const geoJsonRef = useRef<L.GeoJSON | null>(null);

  const getFeatureBounds = (feature: IntersecaoFeature) => {
    const layer = L.geoJSON(feature as any);
    const bounds = layer.getBounds();
    layer.remove();
    return bounds;
  };

  const selectedCenter = useMemo(() => {
    if (!selectedFeature) {
      return null;
    }

    const bounds = getFeatureBounds(selectedFeature);
    return bounds.isValid() ? bounds.getCenter() : null;
  }, [selectedFeature]);

  // Estilo para áreas de intersecção
  const getFeatureStyle = (feature: IntersecaoFeature) => {
    const isSelected = selectedFeature?.properties.id === feature.properties.id;
    return {
      fillColor: '#D32F2F', // Vermelho para desmatamento
      weight: isSelected ? 3 : 2,
      opacity: 1,
      color: isSelected ? '#B71C1C' : '#D32F2F',
      dashArray: '3',
      fillOpacity: isSelected ? 0.8 : 0.5,
    };
  };

  const onEachFeature = (feature: IntersecaoFeature, layer: L.Layer) => {
    const { nomeConservacao, municipio, uf, areaIntersecaoHa } = feature.properties;
    
    const popupContent = `
      <div class="p-3 bg-white rounded-lg shadow-sm">
        <h3 class="font-bold text-sm text-gray-900 mb-2">${nomeConservacao}</h3>
        <div class="text-xs text-gray-700 space-y-1">
          <p><span class="font-semibold">Município:</span> ${municipio}</p>
          <p><span class="font-semibold">UF:</span> ${uf}</p>
          <p><span class="font-semibold">Área:</span> ${areaIntersecaoHa.toFixed(2)} ha</p>
        </div>
      </div>
    `;

    layer.bindPopup(popupContent);
    
    // Adicionar evento de clique
    layer.on('click', () => {
      onFeatureSelect?.(feature);
    });
  };

  useEffect(() => {
    if (!selectedFeature || !mapRef.current || !geoJsonRef.current) {
      return;
    }

    let selectedLayer: L.Layer | null = null;

    geoJsonRef.current.eachLayer((layer) => {
      const feature = (layer as L.Layer & { feature?: IntersecaoFeature }).feature;
      if (!feature) {
        return;
      }

      if ('setStyle' in layer) {
        (layer as L.Path).setStyle(getFeatureStyle(feature));
      }

      if (feature.properties.id === selectedFeature.properties.id) {
        selectedLayer = layer;
      }
    });

    if (!selectedLayer) {
      return;
    }

    const bounds =
      'getBounds' in selectedLayer
        ? (selectedLayer as L.FeatureGroup | L.Polyline | L.Polygon).getBounds()
        : getFeatureBounds(selectedFeature);

    if (bounds.isValid()) {
      mapRef.current.fitBounds(bounds, {
        padding: [48, 48],
        maxZoom: 10,
        animate: true,
      });
    } else if (selectedCenter) {
      mapRef.current.flyTo(selectedCenter, 10);
    }

    (selectedLayer as L.Layer & { openPopup?: () => void }).openPopup?.();
  }, [selectedFeature, data]);

  return (
    <div className="w-full h-full rounded-lg overflow-hidden shadow-sm border border-gray-200">
      <MapContainer
        center={[-15.7942, -52.2319]} // Centro do Brasil
        zoom={4}
        style={{ height: '100%', width: '100%' }}
        ref={mapRef}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />
        
        {data.length > 0 && (
          <GeoJSON
            data={{
              type: 'FeatureCollection',
              features: data,
            } as any}
            style={(feature) => getFeatureStyle(feature as IntersecaoFeature)}
            onEachFeature={(feature, layer) => onEachFeature(feature as IntersecaoFeature, layer)}
            ref={geoJsonRef}
          />
        )}

        {selectedCenter && (
          <CircleMarker
            center={selectedCenter}
            radius={7}
            pathOptions={{
              color: '#FFFFFF',
              fillColor: '#166534',
              fillOpacity: 1,
              weight: 3,
            }}
          />
        )}
      </MapContainer>
    </div>
  );
}
