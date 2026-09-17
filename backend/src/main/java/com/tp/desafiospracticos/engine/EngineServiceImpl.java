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
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.SandboxClient;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalysisResult;
import com.tp.desafiospracticos.engine.staticanalysis.StaticAnalyzer;
import com.tp.desafiospracticos.web.EvaluationRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orquesta el flujo completo: perfil + desafio -> sandbox (dinamicas) + analisis estatico ->
 * gate de compilacion -> dimensiones habilitadas -> agregacion con cap -> feedback -> resultado.
 * <p>
 * Resiliencia: si el sandbox no responde (lanza o no vuelve), las dimensiones estaticas se
 * calculan igual y las dinamicas quedan PENDING_SANDBOX, con status PARTIAL_PENDING y quality
 * null hasta reevaluar.
 */
@Service
public class EngineServiceImpl implements EngineService {

    public static final String ENGINE_VERSION = "1.0.0";
    private static final String DEFAULT_PROFILE_ID = "introductorio";

    private final ProfileRepository profileRepository;
    private final ChallengeRepository challengeRepository;
    private final SandboxClient sandboxClient;
    private final CompilationGate compilationGate;
    private final List<Evaluator> evaluators;
    private final QualityAggregator qualityAggregator;
    private final StaticAnalyzer staticAnalyzer;
    private final FeedbackGenerator feedbackGenerator;

    public EngineServiceImpl(ProfileRepository profileRepository,
                              ChallengeRepository challengeRepository,
                              SandboxClient sandboxClient,
                              CompilationGate compilationGate,
                              List<Evaluator> evaluators,
                              QualityAggregator qualityAggregator,
                              StaticAnalyzer staticAnalyzer,
                              FeedbackGenerator feedbackGenerator) {
        this.profileRepository = profileRepository;
        this.challengeRepository = challengeRepository;
        this.sandboxClient = sandboxClient;
        this.compilationGate = compilationGate;
        this.evaluators = evaluators;
        this.qualityAggregator = qualityAggregator;
        this.staticAnalyzer = staticAnalyzer;
        this.feedbackGenerator = feedbackGenerator;
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest request) {
        String profileId = request.profileId() != null ? request.profileId() : DEFAULT_PROFILE_ID;
        EvaluationProfile profile = profileRepository.findById(profileId);
        Challenge challenge = challengeRepository.findById(request.challengeId());

        // correctness siempre corre: el cap de calidad necesita su subScore aunque su peso sea 0.
        List<Evaluator> enabledEvaluators = evaluators.stream()
                .filter(evaluator -> CorrectnessEvaluator.DIMENSION_ID.equals(evaluator.dimensionId())
                        || profile.weightOf(evaluator.dimensionId()) > 0)
                .toList();

        StaticAnalysisResult staticAnalysis = staticAnalyzer.analyze(request.lenguaje(), request.code());

        ExecutionMetrics metrics;
        try {
            metrics = sandboxClient.run(request.lenguaje(), request.code(), challenge.tests());
        } catch (RuntimeException e) {
            return partialPending(request, profile, challenge, enabledEvaluators, staticAnalysis);
        }

        if (!compilationGate.passes(metrics)) {
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
                request.lenguaje(), request.code(), challenge.tests(), metrics, staticAnalysis, profile
        );

        List<CorrectionDimension> dimensions = enabledEvaluators.stream()
                .map(evaluator -> evaluator.evaluate(context))
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

    private EvaluationResult partialPending(EvaluationRequest request,
                                             EvaluationProfile profile,
                                             Challenge challenge,
                                             List<Evaluator> enabledEvaluators,
                                             StaticAnalysisResult staticAnalysis) {
        EvaluationContext context = new EvaluationContext(
                request.lenguaje(), request.code(), challenge.tests(), null, staticAnalysis, profile
        );

        List<CorrectionDimension> dimensions = enabledEvaluators.stream()
                .map(evaluator -> evaluator.source() == DimensionSource.STATIC
                        ? evaluator.evaluate(context)
                        : pendingDimension(evaluator, profile))
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
                List.of()
        );
    }

    private CorrectionDimension pendingDimension(Evaluator evaluator, EvaluationProfile profile) {
        int weight = profile.weightOf(evaluator.dimensionId());
        return new CorrectionDimension(
                evaluator.dimensionId(), null, weight, 0.0, evaluator.source(), DimensionState.PENDING_SANDBOX, Map.of()
        );
    }
}
