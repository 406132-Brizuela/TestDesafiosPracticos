package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Arranca en 100 y descuenta por anidamiento excesivo, metodos por encima de la complejidad
 * ciclomatica permitida y anti-patrones de loops anidados triples. Umbrales vienen del perfil.
 */
@Component
public class ComplexityEvaluator implements Evaluator {

    public static final String DIMENSION_ID = "complexity";

    private static final int DEFAULT_ANIDAMIENTO_MAX = 3;
    private static final int DEFAULT_COMPLEJIDAD_MAX = 10;
    private static final int PENALIZACION_POR_NIVEL_EXTRA = 15;
    private static final int PENALIZACION_POR_METODO_EXCEDIDO = 10;
    private static final int CAP_PENALIZACION_COMPLEJIDAD = 40;
    private static final int PENALIZACION_ANTIPATRON = 25;

    @Override
    public String dimensionId() {
        return DIMENSION_ID;
    }

    @Override
    public DimensionSource source() {
        return DimensionSource.STATIC;
    }

    @Override
    public CorrectionDimension evaluate(EvaluationContext ctx) {
        Map<String, Object> params = ctx.profile().paramsOf(DIMENSION_ID);
        int anidamientoMax = ParamUtils.intParam(params, "anidamientoMax", DEFAULT_ANIDAMIENTO_MAX);
        int complejidadMax = ParamUtils.intParam(params, "complejidadMax", DEFAULT_COMPLEJIDAD_MAX);

        StaticAnalysisResult analysis = ctx.staticAnalysis();

        int subScore = 100;

        int nivelesExtra = Math.max(0, analysis.nestingDepthMax() - anidamientoMax);
        subScore -= nivelesExtra * PENALIZACION_POR_NIVEL_EXTRA;

        long metodosExcedidos = analysis.complejidadCiclomaticaPorMetodo().values().stream()
                .filter(complejidad -> complejidad > complejidadMax)
                .count();
        int penalizacionComplejidad = (int) Math.min(metodosExcedidos * PENALIZACION_POR_METODO_EXCEDIDO, CAP_PENALIZACION_COMPLEJIDAD);
        subScore -= penalizacionComplejidad;

        List<String> antipatrones = analysis.antipatrones();
        subScore -= antipatrones.size() * PENALIZACION_ANTIPATRON;

        subScore = Math.max(0, Math.min(100, subScore));

        int weight = ctx.profile().weightOf(DIMENSION_ID);
        double contribution = subScore * weight / 100.0;

        Map<String, Object> evidence = Map.of(
                "anidamientoMaxDetectado", analysis.nestingDepthMax(),
                "complejidadCiclomatica", analysis.complejidadCiclomaticaPorMetodo(),
                "antipatrones", antipatrones
        );

        return new CorrectionDimension(DIMENSION_ID, subScore, weight, contribution, DimensionSource.STATIC, DimensionState.OK, evidence);
    }
}
