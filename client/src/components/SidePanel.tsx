import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Loader2, RefreshCw, MapPin, Leaf } from 'lucide-react';

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

interface SidePanelProps {
  selectedFeature?: IntersecaoFeature | null;
  totalAreas: number;
  isLoading?: boolean;
  onRecalculate?: () => void;
  isRecalculating?: boolean;
}

export default function SidePanel({
  selectedFeature,
  totalAreas,
  isLoading = false,
  onRecalculate,
  isRecalculating = false,
}: SidePanelProps) {
  return (
    <div className="flex flex-col h-full bg-gray-50 border-r border-gray-200">
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 bg-green-600 rounded-lg flex items-center justify-center">
            <Leaf className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">SIMDE</h1>
            <p className="text-xs text-gray-600">Monitoramento de Desmatamento</p>
          </div>
        </div>
      </div>

      {/* Conteúdo Principal */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Estatísticas Gerais */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold text-gray-900">Resumo</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Total de Intersecções</span>
              {isLoading ? (
                <Loader2 className="w-4 h-4 animate-spin text-green-600" />
              ) : (
                <span className="text-lg font-bold text-gray-900">{totalAreas}</span>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Detalhes da Feature Selecionada */}
        {selectedFeature ? (
          <Card className="border-green-200 bg-green-50">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-semibold text-gray-900">Detalhes da Área</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-xs font-semibold text-gray-600 uppercase mb-1">Área Protegida</p>
                <p className="text-sm font-bold text-gray-900">{selectedFeature.properties.nomeConservacao}</p>
              </div>

              <div>
                <p className="text-xs font-semibold text-gray-600 uppercase mb-1">Categoria</p>
                <Badge variant="outline" className="bg-white">
                  {selectedFeature.properties.categoria}
                </Badge>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="text-xs font-semibold text-gray-600 uppercase mb-1">Município</p>
                  <p className="text-sm text-gray-900">{selectedFeature.properties.municipio}</p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-gray-600 uppercase mb-1">UF</p>
                  <p className="text-sm text-gray-900">{selectedFeature.properties.uf}</p>
                </div>
              </div>

              <div>
                <p className="text-xs font-semibold text-gray-600 uppercase mb-1">Área de Intersecção</p>
                <p className="text-lg font-bold text-red-600">
                  {selectedFeature.properties.areaIntersecaoHa.toFixed(2)} ha
                </p>
              </div>

              <div className="pt-2 border-t border-green-200">
                <p className="text-xs text-gray-600">
                  <span className="font-semibold">ID Desmatamento:</span> {selectedFeature.properties.codigoDesmatamento}
                </p>
                <p className="text-xs text-gray-600">
                  <span className="font-semibold">ID Conservação:</span> {selectedFeature.properties.codigoConservacao}
                </p>
              </div>
            </CardContent>
          </Card>
        ) : (
          <Card className="border-gray-200 bg-gray-100">
            <CardContent className="pt-6 text-center">
              <MapPin className="w-8 h-8 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-600">Selecione uma área no mapa para ver detalhes</p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Footer com Botão de Recalcular */}
      <div className="p-6 border-t border-gray-200 bg-white">
        <Button
          onClick={onRecalculate}
          disabled={isRecalculating}
          className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold"
        >
          {isRecalculating ? (
            <>
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              Recalculando...
            </>
          ) : (
            <>
              <RefreshCw className="w-4 h-4 mr-2" />
              Recalcular Intersecções
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
