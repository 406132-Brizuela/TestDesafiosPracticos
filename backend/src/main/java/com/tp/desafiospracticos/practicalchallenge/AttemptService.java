package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AttemptService {

    private final AttemptJpaRepository attemptRepository;
    private final PracticalChallengeJpaRepository challengeRepository;

    public AttemptService(AttemptJpaRepository attemptRepository,
                          PracticalChallengeJpaRepository challengeRepository) {
        this.attemptRepository = attemptRepository;
        this.challengeRepository = challengeRepository;
    }

    @Transactional
    public AttemptResponse start(String practicalChallengeId, String userId) {
        PracticalChallengeEntity challenge = challengeRepository.findById(practicalChallengeId)
                .orElseThrow(() -> new PracticalChallengeNotFoundException(practicalChallengeId));
        AttemptEntity attempt = new AttemptEntity(
                UUID.randomUUID().toString(),
                challenge,
                Map.of("code", ""),
                Instant.now(),
                userId
        );
        return toResponse(attemptRepository.save(attempt));
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> findAll(String userId) {
        List<AttemptEntity> attempts = userId == null
                ? attemptRepository.findAllByOrderByCreationDatetimeDesc()
                : attemptRepository.findAllByUserIdOrderByCreationDatetimeDesc(userId);
        return attempts.stream().map(this::toResponse).toList();
    }

    private AttemptResponse toResponse(AttemptEntity attempt) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getPracticalChallenge().getId(),
                attempt.getPracticalChallenge().getTitle(),
                attempt.getCreationDatetime(),
                attempt.getSubmissionDatetime() == null ? "INICIADO" : "ENTREGADO"
        );
    }
}
