import { useEffect, useState } from 'react';
import axios from 'axios';
import MapView from '@/components/MapView';
import SidePanel from '@/components/SidePanel';
import { toast } from 'sonner';

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

/**
 * Design Philosophy: Minimalismo Geográfico com Foco em Dados
 * - Mapa é o protagonista (70% da viewport)
 * - Painel lateral com controles e informações (30%)
 * - Cores semanticamente significativas: verde (proteção), vermelho (desmatamento)
 * - Tipografia IBM Plex Sans para clareza máxima
 * - Transições suaves de 200ms
 */
export default function Home() {
  const [intersecoes, setIntersecoes] = useState<IntersecaoFeature[]>([]);
  const [selectedFeature, setSelectedFeature] = useState<IntersecaoFeature | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRecalculating, setIsRecalculating] = useState(false);

  // URL da API
  const API_BASE_URL = 'http://localhost:8080';

  // Carregar dados de intersecções
  useEffect(() => {
    const fetchIntersecoes = async () => {
      try {
        setIsLoading(true);
        const response = await axios.get(`${API_BASE_URL}/api/intersecoes`);
        
        if (response.data && response.data.features) {
          const features = response.data.features as IntersecaoFeature[];
          setIntersecoes(features);
          setSelectedFeature(features[0] ?? null);
          toast.success(`${response.data.features.length} áreas carregadas com sucesso`);
        }
      } catch (error) {
        console.error('Erro ao carregar intersecções:', error);
        toast.error('Erro ao carregar dados do mapa. Verifique a conexão com a API.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchIntersecoes();
  }, []);

  // Recalcular intersecções
  const handleRecalculate = async () => {
    try {
      setIsRecalculating(true);
      const response = await axios.post(`${API_BASE_URL}/api/intersecoes/recalcular`);
      
      if (response.data) {
        toast.success(`${response.data.novasIntersecoes} novas intersecções calculadas`);
        
        // Recarregar dados
        const dataResponse = await axios.get(`${API_BASE_URL}/api/intersecoes`);
        if (dataResponse.data && dataResponse.data.features) {
          const features = dataResponse.data.features as IntersecaoFeature[];
          setIntersecoes(features);
          setSelectedFeature(features[0] ?? null);
        }
      }
    } catch (error) {
      console.error('Erro ao recalcular intersecções:', error);
      toast.error('Erro ao recalcular intersecções.');
    } finally {
      setIsRecalculating(false);
    }
  };

  return (
    <div className="h-screen w-screen flex bg-white overflow-hidden">
      {/* Painel Lateral */}
      <div className="w-1/3 flex flex-col">
        <SidePanel
          selectedFeature={selectedFeature}
          totalAreas={intersecoes.length}
          isLoading={isLoading}
          onRecalculate={handleRecalculate}
          isRecalculating={isRecalculating}
        />
      </div>

      {/* Mapa */}
      <div className="w-2/3 flex flex-col">
        <MapView
          data={intersecoes}
          selectedFeature={selectedFeature}
          onFeatureSelect={setSelectedFeature}
        />
      </div>
    </div>
  );
}
