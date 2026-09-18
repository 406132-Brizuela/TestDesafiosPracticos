package com.tp.desafiospracticos.motor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

/** Adaptador HTTP para el contrato de lectura expuesto por Motor. */
@Component
public class HttpMotorDesafioClient implements MotorDesafioClient {

    private final RestClient http;

    public HttpMotorDesafioClient(RestClient.Builder builder,
            @Value("${app.motor-url:http://localhost:8081}") String motorUrl) {
        this.http = builder.baseUrl(motorUrl).build();
    }

    @Override
    public MotorChallenge findById(String desafioId) {
        MotorChallenge challenge = http.get()
                .uri("/api/motor/desafios/{id}", desafioId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new MotorChallengeNotFoundException(desafioId);
                        })
                .body(MotorChallenge.class);
        if (challenge == null) {
            throw new IllegalStateException("Motor devolvió una respuesta vacía para " + desafioId);
        }
        return challenge;
    }

    @Override
    public List<MotorChallenge> findAll() {
        return getChallenges(null);
    }

    @Override
    public List<MotorChallenge> findAllByIds(Collection<String> desafioIds) {
        if (desafioIds.isEmpty()) {
            return List.of();
        }
        return getChallenges(String.join(",", desafioIds));
    }

    private List<MotorChallenge> getChallenges(String ids) {
        MotorChallenge[] challenges = http.get()
                .uri(builder -> {
                    builder.path("/api/motor/desafios");
                    if (ids != null) {
                        builder.queryParam("ids", ids);
                    }
                    return builder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        (request, response) -> {
                            throw new IllegalStateException(
                                    "Motor respondió HTTP " + response.getStatusCode().value());
                        })
                .body(MotorChallenge[].class);
        return challenges == null ? List.of() : List.of(challenges);
    }
}
