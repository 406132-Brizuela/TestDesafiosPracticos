package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.aggregation.QualityAggregator;
import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.dimension.Evaluator;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.domain.EvaluationStatus;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.feedback.FeedbackGenerator;
import com.tp.desafiospracticos.engine.gate.CompilationGate;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.SandboxClient;
import com.tp.desafiospracticos.engine.metrics.TestResult;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.PesoDim;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.engine.staticanalysis.JavaStaticAnalyzer;
import com.tp.desafiospracticos.web.EvaluationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineServiceImplTest {

    private static final EvaluationProfile DEFAULT_PROFILE = new EvaluationProfile(
            "default", 1, 50, 50, Map.of("correctness", new PesoDim(100, Map.of()))
    );

    private static final ProfileRepository PROFILE_REPOSITORY = id -> DEFAULT_PROFILE;

    private static List<TestCase> tenTestCases() {
        List<TestCase> tests = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tests.add(new TestCase("case" + i, "input" + i, "expected" + i));
        }
        return tests;
    }

    private static ChallengeRepository challengeRepositoryWith(List<TestCase> tests) {
        Challenge challenge = new Challenge("sample", "consigna", "java", "starter", tests);
        return id -> challenge;
    }

    private static ExecutionMetrics metricsWithPassed(List<TestCase> tests, int passed) {
        List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            TestCase testCase = tests.get(i);
            boolean isPassed = i < passed;
            results.add(new TestResult(testCase.id(), isPassed, testCase.expected(), isPassed ? testCase.expected() : "otro"));
        }
        return new ExecutionMetrics(true, tests.size(), passed, 100L, false, results);
    }

    private static EngineServiceImpl newEngine(SandboxClient sandboxClient, ChallengeRepository challengeRepository) {
        List<Evaluator> evaluators = List.of(new CorrectnessEvaluator());
        return new EngineServiceImpl(
                PROFILE_REPOSITORY,
                challengeRepository,
                sandboxClient,
                new CompilationGate(),
                evaluators,
                new QualityAggregator(),
                new JavaStaticAnalyzer(),
                new FeedbackGenerator()
        );
    }

    @Test
    void apruebaCuandoCorrectnessSuperaElUmbral() {
        List<TestCase> tests = tenTestCases();
        SandboxClient sandbox = (lenguaje, code, t) -> metricsWithPassed(tests, 8);
        EngineServiceImpl engine = newEngine(sandbox, challengeRepositoryWith(tests));

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s1", "sample", "java", "code", "default")
        );

        assertEquals(EvaluationStatus.COMPLETED, result.status());
        assertEquals(80, result.dimensions().get(0).subScore());
        assertEquals(new BigDecimal("80.00"), result.quality());
        assertEquals(Verdict.APPROVED, result.suggestedVerdict());
    }

    @Test
    void aplicaCapCuandoCorrectnessNoAlcanzaElUmbral() {
        List<TestCase> tests = tenTestCases();
        SandboxClient sandbox = (lenguaje, code, t) -> metricsWithPassed(tests, 3);
        EngineServiceImpl engine = newEngine(sandbox, challengeRepositoryWith(tests));

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s2", "sample", "java", "code", "default")
        );

        assertEquals(EvaluationStatus.COMPLETED, result.status());
        assertEquals(30, result.dimensions().get(0).subScore());
        assertEquals(new BigDecimal("30.00"), result.quality());
        assertEquals(Verdict.NOT_APPROVED, result.suggestedVerdict());
    }

    @Test
    void marcaNoCompileCuandoNoCompila() {
        List<TestCase> tests = tenTestCases();
        SandboxClient sandbox = (lenguaje, code, t) -> new ExecutionMetrics(false, tests.size(), 0, 0L, false, List.of());
        EngineServiceImpl engine = newEngine(sandbox, challengeRepositoryWith(tests));

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s3", "sample", "java", "code", "default")
        );

        assertEquals(EvaluationStatus.NO_COMPILE, result.status());
        assertNull(result.quality());
        assertTrue(result.dimensions().isEmpty());
    }
}
