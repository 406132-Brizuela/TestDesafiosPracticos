package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalysisResult;

import java.util.List;

/**
 * {@code metrics} es null cuando el sandbox no respondio (ver resiliencia en EngineServiceImpl):
 * en ese caso solo se invocan evaluadores STATIC, que no lo leen.
 */
public record EvaluationContext(
        String lenguaje,
        String code,
        List<TestCase> tests,
        ExecutionMetrics metrics,
        StaticAnalysisResult staticAnalysis,
        EvaluationProfile profile
) {
}
