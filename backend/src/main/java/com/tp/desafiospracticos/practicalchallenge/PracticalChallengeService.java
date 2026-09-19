package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.motor.MotorChallenge;
import com.tp.desafiospracticos.motor.MotorChallengeResolver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PracticalChallengeService {

    // Mismo default que EngineServiceImpl.DEFAULT_PROFILE_ID: si el desafío no trae rúbrica,
    // queda en el mismo perfil que el engine usaría de todas formas ante un dato faltante.
    private static final String DEFAULT_EVALUATION_PROFILE_ID = "introductorio";

    private final PracticalChallengeJpaRepository repository;
    private final PracticalChallengeCatalog catalog;
    private final MotorChallengeResolver motorResolver;
    private final DesafioContenidoEventPublisher eventPublisher;

    public PracticalChallengeService(PracticalChallengeJpaRepository repository,
                                     PracticalChallengeCatalog catalog,
                                     MotorChallengeResolver motorResolver,
                                     DesafioContenidoEventPublisher eventPublisher) {
        this.repository = repository;
        this.catalog = catalog;
        this.motorResolver = motorResolver;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PracticalChallengeResponse create(PracticalChallengeRequest request, String creatorId) {
        Instant creationDatetime = Instant.now();
        String desafioId = request.desafioId();
        // El id llega desde el redirect de Motor; la consulta HTTP valida que
        // exista y deja a Motor como fuente de verdad de título/dificultad.
        motorResolver.resolveOrThrow(desafioId);

        String evaluationProfileId = request.evaluationProfileId() != null
                ? request.evaluationProfileId()
                : DEFAULT_EVALUATION_PROFILE_ID;

        PracticalChallengeEntity challenge = new PracticalChallengeEntity(
                desafioId,
                catalog.defaultType(),
                request.statement().trim(),
                creatorId,
                creationDatetime,
                evaluationProfileId
        );

        challenge.addFile(new ChallengeFileEntity(
                UUID.randomUUID().toString(),
                ChallengeMainFile.MAIN_FILE_PATH,
                request.starterCode() == null ? "" : request.starterCode(),
                0
        ));

        for (int index = 0; index < request.testCases().size(); index++) {
            PracticalChallengeRequest.TestCaseRequest testCase = request.testCases().get(index);
            TestEntity test = new TestEntity(
                    null,
                    creatorId,
                    creationDatetime,
                    testCase.name().trim(),
                    testCase.input(),
                    testCase.expectedOutput(),
                    testCase.visibility()
            );
            challenge.addVersion(new ChallengeVersionEntity(
                    UUID.randomUUID().toString(), test, 1, index));
        }

        PracticalChallengeEntity saved = repository.save(challenge);
        eventPublisher.publicarContenidoPersistido(desafioId, saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PracticalChallengeSummaryResponse> findAll(String creatorId) {
        List<PracticalChallengeEntity> challenges = creatorId == null
                ? repository.findAllByOrderByCreationDatetimeDesc()
                : repository.findAllByUserCreatorIdOrderByCreationDatetimeDesc(creatorId);
        Map<String, MotorChallenge> motorDataById = motorResolver.resolveBatch(
                challenges.stream().map(PracticalChallengeEntity::getId).toList());
        return challenges.stream().map(challenge -> toSummary(challenge, motorDataById)).toList();
    }

    @Transactional(readOnly = true)
    public PracticalChallengeResponse findById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new PracticalChallengeNotFoundException(id));
    }

    private PracticalChallengeResponse toResponse(PracticalChallengeEntity challenge) {
        MotorChallenge motorData = motorResolver.resolveOrThrow(challenge.getId());
        List<ChallengeVersionEntity> currentTests = currentTests(challenge);
        return new PracticalChallengeResponse(
                challenge.getId(),
                motorData.title(),
                challenge.getStatement(),
                motorData.difficulty(),
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.valueOf(
                        challenge.getChallengeType().getProfile().getLanguage().getName()),
                ChallengeMainFile.contentOf(challenge),
                currentTests.stream()
                        .map(version -> new PracticalChallengeResponse.TestCaseResponse(
                                version.getTest().getId(),
                                version.getTest().getName(),
                                version.getTest().getInput(),
                                version.getTest().getExpectedOutput(),
                                version.getTest().getVisibility()
                        ))
                        .toList()
        );
    }

    private PracticalChallengeSummaryResponse toSummary(
            PracticalChallengeEntity challenge, Map<String, MotorChallenge> motorDataById) {
        MotorChallenge motorData = motorResolver.resolveFromBatchOrFallback(
                motorDataById, challenge.getId());
        return new PracticalChallengeSummaryResponse(
                challenge.getId(),
                motorData.title(),
                motorData.difficulty(),
                challenge.getCreationDatetime(),
                currentTests(challenge).size()
        );
    }

    private List<ChallengeVersionEntity> currentTests(PracticalChallengeEntity challenge) {
        return ChallengeVersions.currentTests(challenge);
    }
}
