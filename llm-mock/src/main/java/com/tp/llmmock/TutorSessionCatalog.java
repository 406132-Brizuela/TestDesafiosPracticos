package com.tp.llmmock;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TutorSessionCatalog {

    public static final String MOCK_RESPONSE =
            "El servicio de tutor IA se encuentra mockeado. Tu mensaje fue recibido, "
                    + "pero todavía no se generan respuestas personalizadas.";

    private final ConcurrentMap<String, TutorSession> sessionsByAttempt = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TutorSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<TutorMessage>> historyBySession = new ConcurrentHashMap<>();

    public TutorSession create(TutorSessionCreateRequest request) {
        TutorSession session = sessionsByAttempt.computeIfAbsent(request.attemptId(), attemptId -> {
            TutorSession created = new TutorSession(
                    stableSessionId(attemptId),
                    attemptId,
                    request.practicalChallengeId(),
                    "ACTIVE",
                    Instant.now()
            );
            sessionsById.put(created.sessionId(), created);
            historyBySession.put(created.sessionId(),
                    Collections.synchronizedList(new ArrayList<>()));
            return created;
        });
        if (!session.practicalChallengeId().equals(request.practicalChallengeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El intento ya está asociado a otro desafío práctico");
        }
        return session;
    }

    public TutorMessage reply(String sessionId, TutorMessageCreateRequest request) {
        TutorSession session = requireSession(sessionId);
        if (!session.attemptId().equals(request.attemptId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La sesión no pertenece al intento indicado");
        }
        if (!session.practicalChallengeId().equals(request.context().practicalChallengeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La sesión no pertenece al desafío práctico indicado");
        }

        List<TutorMessage> history = historyBySession.get(sessionId);
        history.add(message(sessionId, "STUDENT", request.content()));
        TutorMessage response = message(sessionId, "TUTOR", MOCK_RESPONSE);
        history.add(response);
        return response;
    }

    public List<TutorMessage> history(String sessionId) {
        List<TutorMessage> history = historyBySession.get(requireSession(sessionId).sessionId());
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    private TutorSession requireSession(String sessionId) {
        TutorSession session = sessionsById.get(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La sesión del tutor no existe");
        }
        return session;
    }

    private TutorMessage message(String sessionId, String role, String content) {
        return new TutorMessage(UUID.randomUUID().toString(), sessionId, role, content, Instant.now());
    }

    private String stableSessionId(String attemptId) {
        return UUID.nameUUIDFromBytes(("tutor-session:" + attemptId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
}
