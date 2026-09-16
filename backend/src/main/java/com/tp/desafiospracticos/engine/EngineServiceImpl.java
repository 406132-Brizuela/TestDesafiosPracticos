package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.challenge.TestCase;
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
import com.tp.desafiospracticos.engine.metrics.TestResult;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import com.tp.desafiospracticos.engine.profile.ProfileRepository;
import com.tp.desafiospracticos.web.EvaluationRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta el flujo completo: perfil -> sandbox -> gate de compilacion -> dimensiones
 * habilitadas -> agregacion con cap -> resultado.
 */
@Service
public class EngineServiceImpl implements EngineService {

    private static final String DEFAULT_PROFILE_ID = "default";

    private final ProfileRepository profileRepository;
    private final SandboxClient sandboxClient;
    private final CompilationGate compilationGate;
    private final List<Evaluator> evaluators;
    private final QualityAggregator qualityAggregator;

    public EngineServiceImpl(ProfileRepository profileRepository,
                              SandboxClient sandboxClient,
                              CompilationGate compilationGate,
                              List<Evaluator> evaluators,
                              QualityAggregator qualityAggregator) {
        this.profileRepository = profileRepository;
        this.sandboxClient = sandboxClient;
        this.compilationGate = compilationGate;
        this.evaluators = evaluators;
        this.qualityAggregator = qualityAggregator;
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest request) {
        String profileId = request.profileId() != null ? request.profileId() : DEFAULT_PROFILE_ID;
        EvaluationProfile profile = profileRepository.findById(profileId);

        ExecutionMetrics metrics = sandboxClient.run(request.lenguaje(), request.code(), request.tests());
        metrics = applySimulationOverride(metrics, request.tests(), request.simulatedTestsPassed());

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
                request.lenguaje(), request.code(), request.tests(), metrics, profile
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

    /**
     * Hook de testing para el MVP: como {@code StubSandboxClient} siempre simula que pasan
     * todos los casos, este metodo permite forzar cuantos casos pasan a partir del campo
     * opcional {@code EvaluationRequest.simulatedTestsPassed}, para poder probar los distintos
     * caminos (aprobar / reprobar / cap) desde Postman. Se elimina cuando se conecte el
     * sandbox real.
     */
    private ExecutionMetrics applySimulationOverride(ExecutionMetrics metrics, List<TestCase> tests, Integer simulatedTestsPassed) {
        if (simulatedTestsPassed == null) {
            return metrics;
        }

        int total = tests.size();
        int passed = Math.max(0, Math.min(simulatedTestsPassed, total));

        List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            TestCase testCase = tests.get(i);
            boolean isPassed = i < passed;
            results.add(new TestResult(
                    testCase.id(),
                    isPassed,
                    testCase.expected(),
                    isPassed ? testCase.expected() : "simulado-fallo"
            ));
        }

        return new ExecutionMetrics(metrics.compiled(), total, passed, results);
    }
}
