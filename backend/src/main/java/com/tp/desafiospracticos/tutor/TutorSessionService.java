package com.tp.desafiospracticos.tutor;

import com.tp.desafiospracticos.attemptdraft.AttemptDraftEntity;
import com.tp.desafiospracticos.attemptdraft.AttemptDraftJpaRepository;
import com.tp.desafiospracticos.practicalchallenge.AttemptEntity;
import com.tp.desafiospracticos.practicalchallenge.AttemptJpaRepository;
import com.tp.desafiospracticos.practicalchallenge.AttemptNotFoundException;
import com.tp.desafiospracticos.practicalchallenge.ChallengeFileEntity;
import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeEntity;

import org.springframework.stereotype.Service;

@Service
public class TutorSessionService {

    private final AttemptJpaRepository attemptRepository;
    private final AttemptDraftJpaRepository draftRepository;
    private final TutorSessionClient tutorClient;

    public TutorSessionService(AttemptJpaRepository attemptRepository,
                               AttemptDraftJpaRepository draftRepository,
                               TutorSessionClient tutorClient) {
        this.attemptRepository = attemptRepository;
        this.draftRepository = draftRepository;
        this.tutorClient = tutorClient;
    }

    /**
     * Crea la sesión fuera de una transacción de base de datos. El mock LLM usa
     * attemptId como clave idempotente, así un reintento recupera la misma sesión.
     */
    public TutorSessionResponse createForAttempt(String attemptId, String userId) {
        AttemptEntity attempt = findAttempt(attemptId);
        validateAccessAndState(attempt, userId);
        String practicalChallengeId = attempt.getPracticalChallenge().getId();

        TutorSession remoteSession = synchronizeRemoteSession(attempt, practicalChallengeId);

        return new TutorSessionResponse(
                remoteSession.sessionId(),
                attemptId,
                practicalChallengeId,
                remoteSession.status()
        );
    }

    public TutorMessageResponse sendMessage(String attemptId, String userId,
                                            TutorMessageRequest request) {
        AttemptEntity attempt = findAttempt(attemptId);
        validateAccessAndState(attempt, userId);

        String sessionId = attempt.getLlmConversationId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new TutorInteractionNotAllowedException(
                    "El intento todavía no tiene una sesión de tutor activa");
        }

        PracticalChallengeEntity challenge = attempt.getPracticalChallenge();
        String starterCode = starterCodeOf(challenge);
        String currentCode = request.currentCode();
        if (currentCode == null) {
            currentCode = draftRepository.findById(attemptId)
                    .map(AttemptDraftEntity::getContent)
                    .orElse(starterCode);
        }

        TutorMessageCommand command = new TutorMessageCommand(
                attemptId,
                request.content().trim(),
                new TutorMessageCommand.TutorContext(
                        challenge.getId(),
                        challenge.getStatement(),
                        starterCode,
                        currentCode
                )
        );
        TutorMessage message;
        try {
            message = tutorClient.sendMessage(sessionId, command);
        } catch (TutorRemoteSessionNotFoundException exception) {
            TutorSession recoveredSession = synchronizeRemoteSession(attempt, challenge.getId());
            message = tutorClient.sendMessage(recoveredSession.sessionId(), command);
        }
        return new TutorMessageResponse(
                message.messageId(),
                message.sessionId(),
                message.role(),
                message.content(),
                message.createdAt()
        );
    }

    private AttemptEntity findAttempt(String attemptId) {
        return attemptRepository.findWithChallengeById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
    }

    private void validateAccessAndState(AttemptEntity attempt, String userId) {
        if (attempt.getUserId() != null && !attempt.getUserId().equals(userId)) {
            throw new TutorAccessDeniedException();
        }
        if (attempt.getSubmissionDatetime() != null) {
            throw new TutorInteractionNotAllowedException(
                    "El tutor no está disponible para un intento entregado");
        }
    }

    private String starterCodeOf(PracticalChallengeEntity challenge) {
        return challenge.getFiles().stream()
                .filter(file -> "Main.java".equals(file.getPath()))
                .findFirst()
                .or(() -> challenge.getFiles().stream().findFirst())
                .map(ChallengeFileEntity::getContent)
                .orElse("");
    }

    private TutorSession synchronizeRemoteSession(AttemptEntity attempt,
                                                   String practicalChallengeId) {
        TutorSession remoteSession = tutorClient.createSession(attempt.getId(), practicalChallengeId);
        if (attempt.synchronizeTutorSession(remoteSession.sessionId())) {
            attemptRepository.save(attempt);
        }
        return remoteSession;
    }
}
