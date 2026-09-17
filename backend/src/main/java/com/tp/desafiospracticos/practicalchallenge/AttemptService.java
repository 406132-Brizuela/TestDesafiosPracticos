package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.motorstub.MotorStubDataResolver;
import com.tp.desafiospracticos.motorstub.StubDesafioMotorEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    private final AttemptJpaRepository attemptRepository;
    private final PracticalChallengeJpaRepository challengeRepository;
    private final MotorStubDataResolver motorStubResolver;

    public AttemptService(AttemptJpaRepository attemptRepository,
                          PracticalChallengeJpaRepository challengeRepository,
                          MotorStubDataResolver motorStubResolver) {
        this.attemptRepository = attemptRepository;
        this.challengeRepository = challengeRepository;
        this.motorStubResolver = motorStubResolver;
    }

    @Transactional
    public AttemptResponse start(AttemptCreateRequest request, String userId) {
        PracticalChallengeEntity challenge = challengeRepository.findById(request.practicalChallengeId())
                .orElseThrow(() -> new PracticalChallengeNotFoundException(request.practicalChallengeId()));
        AttemptEntity attempt = new AttemptEntity(
                request.intentoId(),
                challenge,
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
        Map<String, StubDesafioMotorEntity> motorDataById = motorStubResolver.resolveBatch(
                attempts.stream().map(attempt -> attempt.getPracticalChallenge().getId()).toList());
        return attempts.stream().map(attempt -> toResponse(attempt, motorDataById)).toList();
    }

    private AttemptResponse toResponse(AttemptEntity attempt) {
        String desafioId = attempt.getPracticalChallenge().getId();
        String title = motorStubResolver.resolveOrThrow(desafioId).getTitle();
        return toResponse(attempt, desafioId, title);
    }

    private AttemptResponse toResponse(AttemptEntity attempt, Map<String, StubDesafioMotorEntity> motorDataById) {
        String desafioId = attempt.getPracticalChallenge().getId();
        String title = motorStubResolver.resolveFromBatchOrFallback(motorDataById, desafioId).getTitle();
        return toResponse(attempt, desafioId, title);
    }

    private AttemptResponse toResponse(AttemptEntity attempt, String desafioId, String title) {
        return new AttemptResponse(
                attempt.getId(),
                desafioId,
                title,
                attempt.getCreationDatetime(),
                attempt.getSubmissionDatetime() == null ? "INICIADO" : "ENTREGADO"
        );
    }
}
