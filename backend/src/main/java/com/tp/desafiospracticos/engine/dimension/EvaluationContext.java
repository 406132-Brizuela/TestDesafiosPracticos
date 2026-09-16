package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;

import java.util.List;

public record EvaluationContext(
        String lenguaje,
        String code,
        List<TestCase> tests,
        ExecutionMetrics metrics,
        EvaluationProfile profile
) {
}
