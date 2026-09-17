package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.constraints.NotBlank;

public record AttemptCreateRequest(
        @NotBlank(message = "El intentoId es obligatorio") String intentoId,
        @NotBlank(message = "El desafío práctico es obligatorio") String practicalChallengeId
) {
}
