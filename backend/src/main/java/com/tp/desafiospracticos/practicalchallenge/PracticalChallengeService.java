package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.motorstub.MotorDesafioClient;
import com.tp.desafiospracticos.motorstub.MotorStubDataResolver;
import com.tp.desafiospracticos.motorstub.StubDesafioMotorEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PracticalChallengeService {

    private final PracticalChallengeJpaRepository repository;
    private final PracticalChallengeCatalog catalog;
    private final MotorDesafioClient motorDesafioClient;
    private final MotorStubDataResolver motorStubResolver;
    private final DesafioContenidoEventPublisher eventPublisher;

    public PracticalChallengeService(PracticalChallengeJpaRepository repository,
                                     PracticalChallengeCatalog catalog,
                                     MotorDesafioClient motorDesafioClient,
                                     MotorStubDataResolver motorStubResolver,
                                     DesafioContenidoEventPublisher eventPublisher) {
        this.repository = repository;
        this.catalog = catalog;
        this.motorDesafioClient = motorDesafioClient;
        this.motorStubResolver = motorStubResolver;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PracticalChallengeResponse create(PracticalChallengeRequest request, String creatorId) {
        Instant creationDatetime = Instant.now();
        String desafioId = request.desafioId();
        motorDesafioClient.registrarDesafioRecibido(
                desafioId, request.title().trim(), request.difficulty());

        PracticalChallengeEntity challenge = new PracticalChallengeEntity(
                desafioId,
                catalog.defaultType(),
                request.statement().trim(),
                creatorId,
                creationDatetime
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
        Map<String, StubDesafioMotorEntity> motorDataById = motorStubResolver.resolveBatch(
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
        StubDesafioMotorEntity motorData = motorStubResolver.resolveOrThrow(challenge.getId());
        List<ChallengeVersionEntity> currentTests = currentTests(challenge);
        return new PracticalChallengeResponse(
                challenge.getId(),
                motorData.getTitle(),
                challenge.getStatement(),
                motorData.getDifficulty(),
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
            PracticalChallengeEntity challenge, Map<String, StubDesafioMotorEntity> motorDataById) {
        StubDesafioMotorEntity motorData = motorStubResolver.resolveFromBatchOrFallback(
                motorDataById, challenge.getId());
        return new PracticalChallengeSummaryResponse(
                challenge.getId(),
                motorData.getTitle(),
                motorData.getDifficulty(),
                challenge.getCreationDatetime(),
                currentTests(challenge).size()
        );
    }

    private List<ChallengeVersionEntity> currentTests(PracticalChallengeEntity challenge) {
        int currentVersion = challenge.getVersions().stream()
                .mapToInt(ChallengeVersionEntity::getVersion)
                .max()
                .orElse(1);
        return challenge.getVersions().stream()
                .filter(version -> version.getVersion() == currentVersion)
                .sorted(Comparator.comparingInt(ChallengeVersionEntity::getTestOrder))
                .toList();
    }
}
