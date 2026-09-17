package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Compara el tiempo de ejecucion contra el limite del perfil (parametro "limiteMs") y
 * clasifica en buckets con tolerancia.
 */
@Component
public class PerformanceEvaluator implements Evaluator {

    public static final String DIMENSION_ID = "performance";
    private static final long DEFAULT_LIMITE_MS = 2000;

    @Override
    public String dimensionId() {
        return DIMENSION_ID;
    }

    @Override
    public DimensionSource source() {
        return DimensionSource.SANDBOX;
    }

    @Override
    public CorrectionDimension evaluate(EvaluationContext ctx) {
        ExecutionMetrics metrics = ctx.metrics();
        Map<String, Object> params = ctx.profile().paramsOf(DIMENSION_ID);
        long limiteMs = ParamUtils.longParam(params, "limiteMs", DEFAULT_LIMITE_MS);
        long tiempoMs = metrics.executionTimeMs();

        int subScore;
        String bucket;
        if (metrics.timedOut() || tiempoMs > limiteMs) {
            subScore = 0;
            bucket = "TIMEOUT";
        } else if (tiempoMs <= limiteMs * 0.25) {
            subScore = 100;
            bucket = "RAPIDO";
        } else if (tiempoMs <= limiteMs * 0.60) {
            subScore = 80;
            bucket = "NORMAL";
        } else {
            subScore = 50;
            bucket = "LENTO";
        }

        int weight = ctx.profile().weightOf(DIMENSION_ID);
        double contribution = subScore * weight / 100.0;

        Map<String, Object> evidence = Map.of(
                "tiempoMs", tiempoMs,
                "limiteMs", limiteMs,
                "bucket", bucket
        );

        return new CorrectionDimension(DIMENSION_ID, subScore, weight, contribution, DimensionSource.SANDBOX, DimensionState.OK, evidence);
    }
}
