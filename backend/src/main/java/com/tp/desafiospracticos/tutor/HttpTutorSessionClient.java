package com.tp.desafiospracticos.tutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpTutorSessionClient implements TutorSessionClient {

    private final RestClient http;

    public HttpTutorSessionClient(RestClient.Builder builder,
            @Value("${app.llm-url:http://localhost:8082}") String llmUrl) {
        this.http = builder.baseUrl(llmUrl).build();
    }

    @Override
    public TutorSession createSession(String attemptId, String practicalChallengeId) {
        try {
            TutorSession session = http.post()
                    .uri("/api/llm/tutor/sessions")
                    .body(new CreateSessionRequest(attemptId, practicalChallengeId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new TutorServiceUnavailableException(
                                "El tutor IA respondió HTTP " + response.getStatusCode().value());
                    })
                    .body(TutorSession.class);
            if (session == null || session.sessionId() == null || session.sessionId().isBlank()) {
                throw new TutorServiceUnavailableException("El tutor IA devolvió una respuesta inválida");
            }
            if (!attemptId.equals(session.attemptId())
                    || !practicalChallengeId.equals(session.practicalChallengeId())) {
                throw new TutorServiceUnavailableException(
                        "El tutor IA devolvió una sesión asociada a otro intento");
            }
            return session;
        } catch (TutorServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new TutorServiceUnavailableException("No se pudo conectar con el tutor IA", exception);
        }
    }

    @Override
    public TutorMessage sendMessage(String sessionId, TutorMessageCommand command) {
        try {
            TutorMessage message = http.post()
                    .uri("/api/llm/tutor/sessions/{sessionId}/messages", sessionId)
                    .body(command)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new TutorRemoteSessionNotFoundException();
                        }
                        throw new TutorServiceUnavailableException(
                                "El tutor IA respondió HTTP " + response.getStatusCode().value());
                    })
                    .body(TutorMessage.class);
            if (message == null || !sessionId.equals(message.sessionId())
                    || message.content() == null || message.content().isBlank()) {
                throw new TutorServiceUnavailableException(
                        "El tutor IA devolvió un mensaje inválido");
            }
            return message;
        } catch (TutorServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new TutorServiceUnavailableException("No se pudo conectar con el tutor IA", exception);
        }
    }

    private record CreateSessionRequest(String attemptId, String practicalChallengeId) {
    }
}
