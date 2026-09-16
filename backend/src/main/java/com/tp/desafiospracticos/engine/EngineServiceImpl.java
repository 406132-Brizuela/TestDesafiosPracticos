package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import com.tp.desafiospracticos.engine.aggregation.AggregationResult;
import com.tp.desafiospracticos.engine.aggregation.QualityAggregator;
import com.tp.desafiospracticos.engine.dimension.EvaluationContext;
import com.tp.desafiospracticos.engine.dimension.Evaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.domain.EvaluationStatus;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.gate.CompilationGate;
import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import com.tp.desafiospracticos.engine.metrics.SandboxClient;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.web.EvaluationRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta el flujo completo: perfil + desafio -> sandbox -> gate de compilacion -> dimensiones
 * habilitadas -> agregacion con cap -> resultado.
 */
@Service
public class EngineServiceImpl implements EngineService {

    private static final String DEFAULT_PROFILE_ID = "default";

    private final ProfileRepository profileRepository;
    private final ChallengeRepository challengeRepository;
    private final SandboxClient sandboxClient;
    private final CompilationGate compilationGate;
    private final List<Evaluator> evaluators;
    private final QualityAggregator qualityAggregator;

    public EngineServiceImpl(ProfileRepository profileRepository,
                              ChallengeRepository challengeRepository,
                              SandboxClient sandboxClient,
                              CompilationGate compilationGate,
                              List<Evaluator> evaluators,
                              QualityAggregator qualityAggregator) {
        this.profileRepository = profileRepository;
        this.challengeRepository = challengeRepository;
        this.sandboxClient = sandboxClient;
        this.compilationGate = compilationGate;
        this.evaluators = evaluators;
        this.qualityAggregator = qualityAggregator;
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest request) {
        String profileId = request.profileId() != null ? request.profileId() : DEFAULT_PROFILE_ID;
        EvaluationProfile profile = profileRepository.findById(profileId);
        Challenge challenge = challengeRepository.findById(request.challengeId());

        ExecutionMetrics metrics = sandboxClient.run(request.lenguaje(), request.code(), challenge.tests());

        if (!compilationGate.passes(metrics)) {
            return new EvaluationResult(
                    request.submissionId(),
                    profile.profileId(),
                    profile.version(),
                    EvaluationStatus.NO_COMPILE,
                    null,
                    Verdict.NOT_APPROVED,
                    profile.correctnessThreshold(),
                    List.of()
            );
        }

        EvaluationContext context = new EvaluationContext(
                request.lenguaje(), request.code(), challenge.tests(), metrics, profile
        );

        List<CorrectionDimension> dimensions = evaluators.stream()
                .map(evaluator -> evaluator.evaluate(context))
                .toList();

        AggregationResult aggregation = qualityAggregator.aggregate(dimensions, profile);

        return new EvaluationResult(
                request.submissionId(),
                profile.profileId(),
                profile.version(),
                EvaluationStatus.COMPLETED,
                aggregation.quality(),
                aggregation.suggestedVerdict(),
                profile.correctnessThreshold(),
                dimensions
        );
    }
}
