package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PracticalChallengeService {

    private final PracticalChallengeJpaRepository repository;
    private final PracticalChallengeCatalog catalog;

    public PracticalChallengeService(PracticalChallengeJpaRepository repository,
                                     PracticalChallengeCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Transactional
    public PracticalChallengeResponse create(PracticalChallengeRequest request, String creatorId) {
        Instant creationDatetime = Instant.now();
        PracticalChallengeEntity challenge = new PracticalChallengeEntity(
                UUID.randomUUID().toString(),
                catalog.defaultType(),
                request.title().trim(),
                request.statement().trim(),
                request.difficulty(),
                request.starterCode() == null ? "" : request.starterCode(),
                creatorId,
                creationDatetime
        );

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

        return toResponse(repository.save(challenge));
    }

    @Transactional(readOnly = true)
    public List<PracticalChallengeSummaryResponse> findAll(String creatorId) {
        List<PracticalChallengeEntity> challenges = creatorId == null
                ? repository.findAllByOrderByCreationDatetimeDesc()
                : repository.findAllByUserCreatorIdOrderByCreationDatetimeDesc(creatorId);
        return challenges.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PracticalChallengeResponse findById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new PracticalChallengeNotFoundException(id));
    }

    private PracticalChallengeResponse toResponse(PracticalChallengeEntity challenge) {
        List<ChallengeVersionEntity> currentTests = currentTests(challenge);
        return new PracticalChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getStatement(),
                challenge.getDifficulty(),
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.valueOf(
                        challenge.getChallengeType().getProfile().getLanguage().getName()),
                challenge.getTemplate(),
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

    private PracticalChallengeSummaryResponse toSummary(PracticalChallengeEntity challenge) {
        return new PracticalChallengeSummaryResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDifficulty(),
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
