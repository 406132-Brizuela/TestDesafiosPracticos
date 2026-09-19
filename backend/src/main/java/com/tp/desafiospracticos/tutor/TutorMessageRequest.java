package com.tp.desafiospracticos.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TutorMessageRequest(
        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String content,

        @Size(max = 100000, message = "El código actual es demasiado grande")
        String currentCode
) {
}
