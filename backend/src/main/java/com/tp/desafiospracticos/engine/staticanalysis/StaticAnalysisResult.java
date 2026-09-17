package com.tp.desafiospracticos.engine.staticanalysis;

import java.util.List;
import java.util.Map;

/**
 * Resultado del analisis estatico del codigo fuente entregado, consumido por las dimensiones
 * complexity y style. No conoce perfiles ni umbrales: es informacion cruda sobre el codigo.
 */
public record StaticAnalysisResult(
        int nestingDepthMax,
        Map<String, Integer> complejidadCiclomaticaPorMetodo,
        List<String> antipatrones,
        List<String> nombresNoDescriptivos,
        Map<String, Integer> lineasPorMetodo
) {

    public static StaticAnalysisResult empty() {
        return new StaticAnalysisResult(0, Map.of(), List.of(), List.of(), Map.of());
    }
}
