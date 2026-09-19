package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import com.tp.desafiospracticos.engine.aggregation.AggregationResult;
import com.tp.desafiospracticos.engine.aggregation.QualityAggregator;
import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.dimension.EvaluationContext;
import com.tp.desafiospracticos.engine.dimension.Evaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;
import com.tp.desafiospracticos.engine.domain.DimensionState;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.domain.EvaluationStatus;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.feedback.FeedbackGenerator;
import com.tp.desafiospracticos.engine.gate.CompilationGate;
import com.tp.desafiospracticos.engine.metrics.CompileCheckResult;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.SandboxClient;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalysisResult;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalyzer;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalyzerRegistry;
import com.tp.desafiospracticos.web.EvaluationRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orquesta el flujo completo: perfil + desafio -> sandbox (dinamicas) + analisis estatico ->
 * gate de compilacion -> dimensiones habilitadas -> agregacion con cap -> feedback -> resultado.
 * <p>
 * Resiliencia: la llamada al sandbox esta acotada por timeout ({@link #callSandbox}), asi que
 * evaluate/compileOnly NUNCA cuelgan ni tiran 500 por una caida de infra. Si el sandbox no
 * responde (excepcion, timeout, lo que sea) las dimensiones ESTATICAS (complexity, style) se
 * calculan igual —no necesitan sandbox, JavaParser solo parsea— y las DINAMICAS (correctness,
 * performance) quedan PENDING_SANDBOX, con status PARTIAL_PENDING y quality null hasta
 * reevaluar. Eso es DISTINTO de NO_COMPILE: ahi el sandbox SI respondio, solo que el codigo no
 * compila.
 * <p>
 * Multi-lenguaje en las dimensiones estaticas: {@link StaticAnalyzerRegistry} resuelve el
 * {@link StaticAnalyzer} por {@code lenguaje}. Si no hay ninguno, complexity/style quedan
 * NOT_APPLICABLE — un tercer estado de dimension, DISTINTO de PENDING_SANDBOX: es FINAL (no
 * hay analizador, no se reintenta nunca), mientras que PENDING_SANDBOX es transitorio (se
 * reintenta cuando el sandbox vuelva). {@link QualityAggregator} reponderar la quality sobre
 * las dimensiones aplicables (con subScore real), asi que ninguno de los dos estados infla ni
 * desinfla el resultado de las que si aplican.
 */
@Service
public class EngineServiceImpl implements EngineService {

    public static final String ENGINE_VERSION = "1.0.0";
    private static final String DEFAULT_PROFILE_ID = "introductorio";
    private static final String PARTIAL_PENDING_MESSAGE =
            "Corrección parcial: la ejecución quedó pendiente; se reintentará cuando el sandbox esté disponible.";

    private static final Logger log = LoggerFactory.getLogger(EngineServiceImpl.class);

    private final ProfileRepository profileRepository;
    private final ChallengeRepository challengeRepository;
    private final SandboxClient sandboxClient;
    private final CompilationGate compilationGate;
    private final List<Evaluator> evaluators;
    private final QualityAggregator qualityAggregator;
    private final StaticAnalyzerRegistry staticAnalyzerRegistry;
    private final FeedbackGenerator feedbackGenerator;
    private final long sandboxTimeoutMs;
    private final ExecutorService sandboxExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public EngineServiceImpl(ProfileRepository profileRepository,
                              ChallengeRepository challengeRepository,
                              SandboxClient sandboxClient,
                              CompilationGate compilationGate,
                              List<Evaluator> evaluators,
                              QualityAggregator qualityAggregator,
                              StaticAnalyzerRegistry staticAnalyzerRegistry,
                              FeedbackGenerator feedbackGenerator,
                              @Value("${engine.sandbox.timeout-ms:20000}") long sandboxTimeoutMs) {
        this.profileRepository = profileRepository;
        this.challengeRepository = challengeRepository;
        this.sandboxClient = sandboxClient;
        this.compilationGate = compilationGate;
        this.evaluators = evaluators;
        this.qualityAggregator = qualityAggregator;
        this.staticAnalyzerRegistry = staticAnalyzerRegistry;
        this.feedbackGenerator = feedbackGenerator;
        this.sandboxTimeoutMs = sandboxTimeoutMs;
    }

    @PreDestroy
    void shutdown() {
        sandboxExecutor.shutdownNow();
    }

    // Sin profile, sin challenge, sin static analysis, sin aggregation: compilar no es evaluar.
    @Override
    public CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files) {
        return callSandbox("compileOnly", () -> sandboxClient.compileOnly(lenguaje, files))
                .orElseGet(CompileCheckResult::sandboxUnavailable);
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest request) {
        // El perfil de evaluación es un dato DEL DESAFÍO (Challenge.evaluationProfileId),
        // resuelto acá por challengeId — no algo que decida quien llama. request.profileId()
        // queda sin usar (compat): el front ya no lo manda y aunque lo mandara se ignora.
        Challenge challenge = challengeRepository.findById(request.challengeId());
        String profileId = challenge.evaluationProfileId() != null
                ? challenge.evaluationProfileId()
                : DEFAULT_PROFILE_ID;
        EvaluationProfile profile = profileRepository.findById(profileId);

        // correctness siempre corre: el cap de calidad necesita su subScore aunque su peso sea 0.
        List<Evaluator> enabledEvaluators = evaluators.stream()
                .filter(evaluator -> CorrectnessEvaluator.DIMENSION_ID.equals(evaluator.dimensionId())
                        || profile.weightOf(evaluator.dimensionId()) > 0)
                .toList();

        List<SourceFile> files = SourceFile.resolve(request.code(), request.files());
        Optional<StaticAnalyzer> staticAnalyzer = staticAnalyzerRegistry.forLanguage(request.lenguaje());
        StaticAnalysisResult staticAnalysis = staticAnalyzer
                .map(analyzer -> safeStaticAnalysis(analyzer, request.lenguaje(), files))
                .orElse(StaticAnalysisResult.empty());
        boolean staticApplicable = staticAnalyzer.isPresent();

        Optional<ExecutionMetrics> metrics = callSandbox("run",
                () -> sandboxClient.run(request.lenguaje(), files, challenge.tests()));
        if (metrics.isEmpty()) {
            return partialPending(request, profile, challenge, enabledEvaluators, staticAnalysis, files, staticApplicable);
        }

        if (!compilationGate.passes(metrics.get())) {
            return new EvaluationResult(
                    request.submissionId(),
                    profile.profileId(),
                    profile.version(),
                    ENGINE_VERSION,
                    EvaluationStatus.NO_COMPILE,
                    null,
                    Verdict.NOT_APPROVED,
                    profile.approvalThreshold(),
                    List.of(),
                    List.of()
            );
        }

        EvaluationContext context = new EvaluationContext(
                request.lenguaje(), files, challenge.tests(), metrics.get(), staticAnalysis, profile
        );

        List<CorrectionDimension> dimensions = enabledEvaluators.stream()
                .map(evaluator -> evaluateDimension(evaluator, context, profile, true, staticApplicable))
                .toList();

        AggregationResult aggregation = qualityAggregator.aggregate(dimensions, profile);
        List<String> feedback = feedbackGenerator.generate(dimensions);

        return new EvaluationResult(
                request.submissionId(),
                profile.profileId(),
                profile.version(),
                ENGINE_VERSION,
                EvaluationStatus.COMPLETED,
                aggregation.quality(),
                aggregation.suggestedVerdict(),
                profile.approvalThreshold(),
                dimensions,
                feedback
        );
    }

    /**
     * JavaParser solo parsea (no ejecuta), pero igual no confiamos en que un fuente
     * arbitrario nunca la rompa: si el analisis estatico falla por lo que sea, degradamos a
     * "sin datos estaticos" en vez de tirar la evaluacion entera.
     */
    private StaticAnalysisResult safeStaticAnalysis(StaticAnalyzer analyzer, String lenguaje, List<SourceFile> files) {
        try {
            return analyzer.analyze(lenguaje, files);
        } catch (RuntimeException e) {
            log.warn("Analisis estatico fallo sobre el submission; se continua sin datos estaticos", e);
            return StaticAnalysisResult.empty();
        }
    }

    /**
     * Une los dos ejes de "no se puede calcular esta dimension": si no aplica un analizador
     * estatico para el lenguaje (NOT_APPLICABLE, final) o si el sandbox no respondio para las
     * dinamicas (PENDING_SANDBOX, transitorio). Cuando la fuente SI esta disponible, delega en
     * el evaluator real.
     */
    private CorrectionDimension evaluateDimension(Evaluator evaluator,
                                                   EvaluationContext context,
                                                   EvaluationProfile profile,
                                                   boolean dynamicAvailable,
                                                   boolean staticApplicable) {
        if (evaluator.source() == DimensionSource.STATIC) {
            return staticApplicable ? evaluator.evaluate(context) : notApplicableDimension(evaluator, profile);
        }
        return dynamicAvailable ? evaluator.evaluate(context) : pendingDimension(evaluator, profile);
    }

    /**
     * Corre {@code action} (una llamada al sandbox) acotada por {@link #sandboxTimeoutMs}, en
     * un hilo aparte para que un sandbox colgado (deadlock, socket sin timeout, lo que sea)
     * nunca cuelgue el request. Devuelve {@code empty()} ante CUALQUIER falla de infra
     * (excepcion, timeout, interrupcion) — el llamador decide que hacer con eso (PARTIAL_PENDING
     * o "sandbox no disponible"), nunca deja escapar la excepcion como un 500.
     */
    private <T> Optional<T> callSandbox(String operacion, Callable<T> action) {
        Future<T> future = sandboxExecutor.submit(action);
        try {
            return Optional.of(future.get(sandboxTimeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Sandbox no respondio dentro de {} ms en '{}'; se degrada", sandboxTimeoutMs, operacion);
            return Optional.empty();
        } catch (ExecutionException e) {
            log.warn("Sandbox fallo en '{}'; se degrada", operacion, e.getCause() != null ? e.getCause() : e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrumpido esperando al sandbox en '{}'; se degrada", operacion);
            return Optional.empty();
        }
    }

    private EvaluationResult partialPending(EvaluationRequest request,
                                             EvaluationProfile profile,
                                             Challenge challenge,
                                             List<Evaluator> enabledEvaluators,
                                             StaticAnalysisResult staticAnalysis,
                                             List<SourceFile> files,
                                             boolean staticApplicable) {
        EvaluationContext context = new EvaluationContext(
                request.lenguaje(), files, challenge.tests(), null, staticAnalysis, profile
        );

        List<CorrectionDimension> dimensions = enabledEvaluators.stream()
                .map(evaluator -> evaluateDimension(evaluator, context, profile, false, staticApplicable))
                .toList();

        return new EvaluationResult(
                request.submissionId(),
                profile.profileId(),
                profile.version(),
                ENGINE_VERSION,
                EvaluationStatus.PARTIAL_PENDING,
                null,
                Verdict.PENDING,
                profile.approvalThreshold(),
                dimensions,
                List.of(PARTIAL_PENDING_MESSAGE)
        );
    }

    private CorrectionDimension pendingDimension(Evaluator evaluator, EvaluationProfile profile) {
        int weight = profile.weightOf(evaluator.dimensionId());
        return new CorrectionDimension(
                evaluator.dimensionId(), null, weight, 0.0, evaluator.source(), DimensionState.PENDING_SANDBOX, Map.of()
        );
    }

    private CorrectionDimension notApplicableDimension(Evaluator evaluator, EvaluationProfile profile) {
        int weight = profile.weightOf(evaluator.dimensionId());
        return new CorrectionDimension(
                evaluator.dimensionId(), null, weight, 0.0, evaluator.source(), DimensionState.NOT_APPLICABLE, Map.of()
        );
    }
}
