package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.aggregation.QualityAggregator;
import com.tp.desafiospracticos.engine.dimension.ComplexityEvaluator;
import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.dimension.Evaluator;
import com.tp.desafiospracticos.engine.dimension.PerformanceEvaluator;
import com.tp.desafiospracticos.engine.dimension.StyleEvaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.domain.EvaluationStatus;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.feedback.FeedbackGenerator;
import com.tp.desafiospracticos.engine.gate.CompilationGate;
import com.tp.desafiospracticos.engine.metrics.CompileCheckResult;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.SandboxClient;
import com.tp.desafiospracticos.engine.metrics.TestResult;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.PesoDim;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.engine.staticanalysis.JavaStaticAnalyzer;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalyzerRegistry;
import com.tp.desafiospracticos.web.EvaluationRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineServiceImplTest {

    private static final long DEFAULT_TIMEOUT_MS = 5_000;

    private static final EvaluationProfile DEFAULT_PROFILE = new EvaluationProfile(
            "default", 1, 50, 50, Map.of("correctness", new PesoDim(100, Map.of()))
    );

    private static final ProfileRepository PROFILE_REPOSITORY = id -> DEFAULT_PROFILE;

    // Perfil con las 4 dimensiones habilitadas, para poder distinguir en el mismo resultado
    // cuales quedan calculadas (estaticas) y cuales PENDING_SANDBOX (dinamicas).
    private static final EvaluationProfile FULL_PROFILE = new EvaluationProfile(
            "full", 1, 50, 50, Map.of(
                    "correctness", new PesoDim(40, Map.of()),
                    "complexity", new PesoDim(30, Map.of()),
                    "performance", new PesoDim(15, Map.of()),
                    "style", new PesoDim(15, Map.of())
            )
    );

    private static final ProfileRepository FULL_PROFILE_REPOSITORY = id -> FULL_PROFILE;

    // Solo Java tiene analizador registrado — "python"/"javascript"/etc. no matchean ninguno,
    // que es justamente lo que ejercitan los tests de NOT_APPLICABLE.
    private static final StaticAnalyzerRegistry JAVA_ONLY_REGISTRY =
            new StaticAnalyzerRegistry(List.of(new JavaStaticAnalyzer()));

    private static List<TestCase> tenTestCases() {
        List<TestCase> tests = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tests.add(new TestCase("case" + i, "input" + i, "expected" + i));
        }
        return tests;
    }

    private static ChallengeRepository challengeRepositoryWith(List<TestCase> tests) {
        // PROFILE_REPOSITORY es un stub que ignora el id y siempre devuelve DEFAULT_PROFILE,
        // así que evaluationProfileId acá es solo para que el Challenge quede completo.
        Challenge challenge = new Challenge("sample", "consigna", "java", "starter", tests, "default");
        return id -> challenge;
    }

    private static ExecutionMetrics metricsWithPassed(List<TestCase> tests, int passed) {
        return metricsWithPassed(tests, passed, 100L);
    }

    private static ExecutionMetrics metricsWithPassed(List<TestCase> tests, int passed, long executionTimeMs) {
        List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            TestCase testCase = tests.get(i);
            boolean isPassed = i < passed;
            results.add(new TestResult(testCase.id(), isPassed, testCase.expected(), isPassed ? testCase.expected() : "otro"));
        }
        return new ExecutionMetrics(true, tests.size(), passed, executionTimeMs, false, results);
    }

    // SandboxClient dejó de ser functional interface al sumar compileOnly (POST /engine/compile);
    // este helper arma un stub de "run" sin tener que implementar compileOnly en cada test.
    private static SandboxClient sandboxRunning(java.util.function.Supplier<ExecutionMetrics> metrics) {
        return new SandboxClient() {
            @Override
            public ExecutionMetrics run(String lenguaje, List<SourceFile> files, List<TestCase> tests) {
                return metrics.get();
            }

            @Override
            public CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files) {
                throw new UnsupportedOperationException("compileOnly no se usa en este test");
            }
        };
    }

    /** Simula un sandbox caído: cualquier llamada a run() explota. */
    private static SandboxClient sandboxThrowing() {
        return new SandboxClient() {
            @Override
            public ExecutionMetrics run(String lenguaje, List<SourceFile> files, List<TestCase> tests) {
                throw new IllegalStateException("sandbox no disponible");
            }

            @Override
            public CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files) {
                throw new IllegalStateException("sandbox no disponible");
            }
        };
    }

    /** Simula un sandbox colgado: run() nunca vuelve dentro del timeout configurado. */
    private static SandboxClient sandboxHanging() {
        return new SandboxClient() {
            @Override
            public ExecutionMetrics run(String lenguaje, List<SourceFile> files, List<TestCase> tests) {
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return metricsWithPassed(tests, tests.size());
            }

            @Override
            public CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files) {
                throw new UnsupportedOperationException("compileOnly no se usa en este test");
            }
        };
    }

    private static EngineServiceImpl newEngine(SandboxClient sandboxClient, ChallengeRepository challengeRepository) {
        return newEngine(sandboxClient, challengeRepository, List.of(new CorrectnessEvaluator()),
                PROFILE_REPOSITORY, DEFAULT_TIMEOUT_MS);
    }

    private static EngineServiceImpl newEngine(SandboxClient sandboxClient,
                                                ChallengeRepository challengeRepository,
                                                List<Evaluator> evaluators,
                                                ProfileRepository profileRepository,
                                                long sandboxTimeoutMs) {
        return new EngineServiceImpl(
                profileRepository,
                challengeRepository,
                sandboxClient,
                new CompilationGate(),
                evaluators,
                new QualityAggregator(),
                JAVA_ONLY_REGISTRY,
                new FeedbackGenerator(),
                sandboxTimeoutMs
        );
    }

    @Test
    void apruebaCuandoCorrectnessSuperaElUmbral() {
        List<TestCase> tests = tenTestCases();
        SandboxClient sandbox = sandboxRunning(() -> metricsWithPassed(tests, 8));
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
        SandboxClient sandbox = sandboxRunning(() -> metricsWithPassed(tests, 3));
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
        SandboxClient sandbox = sandboxRunning(() -> new ExecutionMetrics(false, tests.size(), 0, 0L, false, List.of()));
        EngineServiceImpl engine = newEngine(sandbox, challengeRepositoryWith(tests));

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s3", "sample", "java", "code", "default")
        );

        assertEquals(EvaluationStatus.NO_COMPILE, result.status());
        assertNull(result.quality());
        assertTrue(result.dimensions().isEmpty());
    }

    @Test
    void marcaPartialPendingCuandoElSandboxTiraExcepcion() {
        List<TestCase> tests = tenTestCases();
        List<Evaluator> evaluators = List.of(
                new CorrectnessEvaluator(), new ComplexityEvaluator(), new StyleEvaluator(), new PerformanceEvaluator());
        EngineServiceImpl engine = newEngine(
                sandboxThrowing(), challengeRepositoryWith(tests), evaluators, FULL_PROFILE_REPOSITORY, DEFAULT_TIMEOUT_MS);

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s4", "sample", "java",
                        "public class Main { public static void main(String[] args) { } }", "default")
        );

        assertEquals(EvaluationStatus.PARTIAL_PENDING, result.status());
        assertNull(result.quality());
        assertEquals(Verdict.PENDING, result.suggestedVerdict());
        assertFalse(result.feedbackAlumno().isEmpty());

        Map<String, CorrectionDimension> byDimension = result.dimensions().stream()
                .collect(Collectors.toMap(CorrectionDimension::dimension, Function.identity()));

        // Estáticas: se calcularon igual, no necesitan sandbox.
        assertEquals(DimensionState.OK, byDimension.get("complexity").state());
        assertEquals(100, byDimension.get("complexity").subScore());
        assertEquals(DimensionState.OK, byDimension.get("style").state());
        assertEquals(100, byDimension.get("style").subScore());

        // Dinámicas: pendientes, sin subScore, no cuentan para quality.
        assertEquals(DimensionState.PENDING_SANDBOX, byDimension.get("correctness").state());
        assertNull(byDimension.get("correctness").subScore());
        assertEquals(DimensionState.PENDING_SANDBOX, byDimension.get("performance").state());
        assertNull(byDimension.get("performance").subScore());
    }

    @Test
    void marcaPartialPendingCuandoElSandboxNoRespondeATiempo() {
        List<TestCase> tests = tenTestCases();
        EngineServiceImpl engine = newEngine(
                sandboxHanging(), challengeRepositoryWith(tests), List.of(new CorrectnessEvaluator()),
                PROFILE_REPOSITORY, 100L);

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s5", "sample", "java", "code", "default")
        );

        assertEquals(EvaluationStatus.PARTIAL_PENDING, result.status());
        assertNull(result.quality());
        assertEquals(DimensionState.PENDING_SANDBOX, result.dimensions().get(0).state());
    }

    @Test
    void enJavaLasCuatroDimensionesAplicanYLaReponderacionEsUnNoOp() {
        List<TestCase> tests = tenTestCases();
        List<Evaluator> evaluators = List.of(
                new CorrectnessEvaluator(), new ComplexityEvaluator(), new StyleEvaluator(), new PerformanceEvaluator());
        // 1500ms cae en el bucket LENTO (entre 60% y 100% del limiteMs=2000 default) -> subScore 50.
        SandboxClient sandbox = sandboxRunning(() -> metricsWithPassed(tests, 8, 1500L));
        EngineServiceImpl engine = newEngine(
                sandbox, challengeRepositoryWith(tests), evaluators, FULL_PROFILE_REPOSITORY, DEFAULT_TIMEOUT_MS);

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s6", "sample", "java",
                        "public class Main { public static void main(String[] args) { } }", "default")
        );

        assertEquals(EvaluationStatus.COMPLETED, result.status());
        Map<String, CorrectionDimension> byDimension = result.dimensions().stream()
                .collect(Collectors.toMap(CorrectionDimension::dimension, Function.identity()));
        assertEquals(DimensionState.OK, byDimension.get("complexity").state());
        assertEquals(DimensionState.OK, byDimension.get("style").state());
        assertEquals(DimensionState.OK, byDimension.get("correctness").state());
        assertEquals(DimensionState.OK, byDimension.get("performance").state());

        // pesos: correctness 40 + complexity 30 + performance 15 + style 15 = 100, las 4 aplican
        // -> dividir por la suma de pesos aplicables (100) o por 100 fijo da lo mismo: no-op.
        assertEquals(new BigDecimal("84.50"), result.quality());
    }

    @Test
    void reponderaQualitySobreDimensionesAplicablesCuandoNoHayAnalizadorParaElLenguaje() {
        List<TestCase> tests = tenTestCases();
        List<Evaluator> evaluators = List.of(
                new CorrectnessEvaluator(), new ComplexityEvaluator(), new StyleEvaluator(), new PerformanceEvaluator());
        SandboxClient sandbox = sandboxRunning(() -> metricsWithPassed(tests, 8, 1500L));
        EngineServiceImpl engine = newEngine(
                sandbox, challengeRepositoryWith(tests), evaluators, FULL_PROFILE_REPOSITORY, DEFAULT_TIMEOUT_MS);

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s7", "sample", "python", "print('hola')", "default")
        );

        assertEquals(EvaluationStatus.COMPLETED, result.status());
        Map<String, CorrectionDimension> byDimension = result.dimensions().stream()
                .collect(Collectors.toMap(CorrectionDimension::dimension, Function.identity()));

        // No hay analizador para "python": estaticas quedan NOT_APPLICABLE, sin subScore.
        assertEquals(DimensionState.NOT_APPLICABLE, byDimension.get("complexity").state());
        assertNull(byDimension.get("complexity").subScore());
        assertEquals(DimensionState.NOT_APPLICABLE, byDimension.get("style").state());
        assertNull(byDimension.get("style").subScore());

        // Dinamicas: no dependen del analizador estatico, se calculan igual.
        assertEquals(DimensionState.OK, byDimension.get("correctness").state());
        assertEquals(80, byDimension.get("correctness").subScore());
        assertEquals(DimensionState.OK, byDimension.get("performance").state());
        assertEquals(50, byDimension.get("performance").subScore());

        // quality reponderada solo sobre correctness(40) + performance(15): (80*40+50*15)/55.
        assertEquals(new BigDecimal("71.82"), result.quality());
        assertEquals(Verdict.APPROVED, result.suggestedVerdict());
    }

    @Test
    void distingueNotApplicableDePendingSandboxEnElMismoResultado() {
        List<TestCase> tests = tenTestCases();
        List<Evaluator> evaluators = List.of(
                new CorrectnessEvaluator(), new ComplexityEvaluator(), new StyleEvaluator(), new PerformanceEvaluator());
        EngineServiceImpl engine = newEngine(
                sandboxThrowing(), challengeRepositoryWith(tests), evaluators, FULL_PROFILE_REPOSITORY, DEFAULT_TIMEOUT_MS);

        EvaluationResult result = engine.evaluate(
                new EvaluationRequest("s8", "sample", "python", "print('hola')", "default")
        );

        // Sin analizador para "python" Y con el sandbox caído: las estaticas son NOT_APPLICABLE
        // (final, no se reintenta) y las dinamicas PENDING_SANDBOX (transitorio) — dos motivos
        // distintos para "sin subScore", nunca confundidos entre si.
        assertEquals(EvaluationStatus.PARTIAL_PENDING, result.status());
        assertNull(result.quality());

        Map<String, CorrectionDimension> byDimension = result.dimensions().stream()
                .collect(Collectors.toMap(CorrectionDimension::dimension, Function.identity()));
        assertEquals(DimensionState.NOT_APPLICABLE, byDimension.get("complexity").state());
        assertEquals(DimensionState.NOT_APPLICABLE, byDimension.get("style").state());
        assertEquals(DimensionState.PENDING_SANDBOX, byDimension.get("correctness").state());
        assertEquals(DimensionState.PENDING_SANDBOX, byDimension.get("performance").state());
    }

    @Test
    void compileOnlyDevuelveSandboxNoDisponibleCuandoElSandboxFalla() {
        CompileCheckResult result = newEngine(sandboxThrowing(), challengeRepositoryWith(tenTestCases()))
                .compileOnly("java", SourceFile.resolve("public class Main {}", null));

        assertFalse(result.compiles());
        assertFalse(result.sandboxAvailable());
        assertFalse(result.diagnostics().isEmpty());
    }
}
