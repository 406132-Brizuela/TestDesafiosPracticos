package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Arranca en 100 y descuenta por nombres poco descriptivos y metodos demasiado largos.
 */
@Component
public class StyleEvaluator implements Evaluator {

    public static final String DIMENSION_ID = "style";

    private static final int LINEAS_METODO_MAX = 40;
    private static final int PENALIZACION_POR_NOMBRE = 5;
    private static final int CAP_PENALIZACION_NOMBRES = 30;
    private static final int PENALIZACION_POR_METODO_LARGO = 10;
    private static final int CAP_PENALIZACION_METODOS = 30;

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
        StaticAnalysisResult analysis = ctx.staticAnalysis();

        int subScore = 100;

        List<String> nombresNoDescriptivos = analysis.nombresNoDescriptivos();
        int penalizacionNombres = Math.min(nombresNoDescriptivos.size() * PENALIZACION_POR_NOMBRE, CAP_PENALIZACION_NOMBRES);
        subScore -= penalizacionNombres;

        List<String> metodosLargos = analysis.lineasPorMetodo().entrySet().stream()
                .filter(entry -> entry.getValue() > LINEAS_METODO_MAX)
                .map(Map.Entry::getKey)
                .toList();
        int penalizacionMetodos = Math.min(metodosLargos.size() * PENALIZACION_POR_METODO_LARGO, CAP_PENALIZACION_METODOS);
        subScore -= penalizacionMetodos;

        subScore = Math.max(0, Math.min(100, subScore));

        int weight = ctx.profile().weightOf(DIMENSION_ID);
        double contribution = subScore * weight / 100.0;

        Map<String, Object> evidence = Map.of(
                "nombresNoDescriptivos", nombresNoDescriptivos,
                "metodosLargos", metodosLargos
        );

        return new CorrectionDimension(DIMENSION_ID, subScore, weight, contribution, DimensionSource.STATIC, DimensionState.OK, evidence);
    }
}
