package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.constraints.NotNull;

public record AttemptDraftSaveRequest(
        @NotNull(message = "El contenido es obligatorio") String content
) {
}
