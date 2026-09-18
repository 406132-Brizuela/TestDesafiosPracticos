package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.attemptdraft.AttemptDraftEntity;
import com.tp.desafiospracticos.attemptdraft.AttemptDraftJpaRepository;
import com.tp.desafiospracticos.motor.MotorChallenge;
import com.tp.desafiospracticos.motor.MotorChallengeResolver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    private final AttemptJpaRepository attemptRepository;
    private final PracticalChallengeJpaRepository challengeRepository;
    private final MotorChallengeResolver motorResolver;
    private final AttemptDraftJpaRepository draftRepository;

    public AttemptService(AttemptJpaRepository attemptRepository,
                          PracticalChallengeJpaRepository challengeRepository,
                          MotorChallengeResolver motorResolver,
                          AttemptDraftJpaRepository draftRepository) {
        this.attemptRepository = attemptRepository;
        this.challengeRepository = challengeRepository;
        this.motorResolver = motorResolver;
        this.draftRepository = draftRepository;
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
        Map<String, MotorChallenge> motorDataById = motorResolver.resolveBatch(
                attempts.stream().map(attempt -> attempt.getPracticalChallenge().getId()).toList());
        return attempts.stream().map(attempt -> toResponse(attempt, motorDataById)).toList();
    }

    // userId se recibe por consistencia con el resto de la API (mismo shape
    // que findAll) pero todavía no se usa para restringir ownership del
    // intento/borrador — ver "Fuera de alcance" del guardado de borrador:
    // el perfil local no tiene auth real y no se endurece acá.
    @Transactional(readOnly = true)
    public AttemptDetailResponse findById(String attemptId, String userId) {
        AttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
        return toDetailResponse(attempt);
    }

    @Transactional
    public AttemptDetailResponse saveDraft(String attemptId, String content, String userId) {
        AttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        Instant now = Instant.now();
        draftRepository.findById(attemptId)
                .ifPresentOrElse(
                        draft -> draft.updateContent(content, now),
                        () -> draftRepository.save(new AttemptDraftEntity(attemptId, content, now))
                );

        return toDetailResponse(attempt);
    }

    private AttemptDetailResponse toDetailResponse(AttemptEntity attempt) {
        PracticalChallengeEntity challenge = attempt.getPracticalChallenge();
        String desafioId = challenge.getId();
        String title = motorResolver.resolveOrThrow(desafioId).title();
        String draftCode = draftRepository.findById(attempt.getId())
                .map(AttemptDraftEntity::getContent)
                .orElse(null);

        return new AttemptDetailResponse(
                attempt.getId(),
                desafioId,
                title,
                challenge.getStatement(),
                ChallengeMainFile.contentOf(challenge),
                draftCode,
                attempt.getSubmissionDatetime() == null ? "INICIADO" : "ENTREGADO",
                attempt.getCreationDatetime()
        );
    }

    private AttemptResponse toResponse(AttemptEntity attempt) {
        String desafioId = attempt.getPracticalChallenge().getId();
        String title = motorResolver.resolveOrThrow(desafioId).title();
        return toResponse(attempt, desafioId, title);
    }

    private AttemptResponse toResponse(AttemptEntity attempt, Map<String, MotorChallenge> motorDataById) {
        String desafioId = attempt.getPracticalChallenge().getId();
        String title = motorResolver.resolveFromBatchOrFallback(motorDataById, desafioId).title();
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
