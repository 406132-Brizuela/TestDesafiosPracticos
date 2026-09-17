package com.tp.desafiospracticos.practicalchallenge;

import java.time.Instant;

/**
 * Detalle completo de un intento para la pantalla de resolución del
 * alumno. A diferencia de {@link AttemptResponse} (DTO liviano del
 * listado), incluye la consigna, el código inicial del desafío y el
 * borrador guardado (si existe). {@code draftCode} es {@code null} cuando
 * el alumno todavía no guardó nada — el frontend usa {@code starterCode}
 * como valor inicial del editor en ese caso.
 */
public record AttemptDetailResponse(
        String id,
        String practicalChallengeId,
        String challengeTitle,
        String statement,
        String starterCode,
        String draftCode,
        String status,
        Instant creationDatetime
) {
}
