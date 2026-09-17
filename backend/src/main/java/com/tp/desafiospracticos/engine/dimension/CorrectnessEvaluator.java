package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.TestResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CorrectnessEvaluator implements Evaluator {

    public static final String DIMENSION_ID = "correctness";

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
        int testsTotal = metrics.testsTotal();
        int testsPassed = metrics.testsPassed();

        int subScore = testsTotal == 0 ? 0 : (int) Math.round((testsPassed * 100.0) / testsTotal);

        int weight = ctx.profile().weightOf(DIMENSION_ID);
        double contribution = subScore * weight / 100.0;

        List<String> fallados = metrics.results().stream()
                .filter(result -> !result.passed())
                .map(TestResult::caseId)
                .toList();

        Map<String, Object> evidence = Map.of(
                "testsTotal", testsTotal,
                "testsPassed", testsPassed,
                "fallados", fallados
        );

        return new CorrectionDimension(DIMENSION_ID, subScore, weight, contribution, DimensionSource.SANDBOX, DimensionState.OK, evidence);
    }
}
