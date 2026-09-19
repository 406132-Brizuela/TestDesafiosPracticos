package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PracticalChallengeRequest(
        @NotBlank(message = "El desafioId es obligatorio") String desafioId,
        @NotBlank(message = "El título es obligatorio") String title,
        @NotBlank(message = "La consigna es obligatoria") String statement,
        @NotNull(message = "La dificultad es obligatoria") Difficulty difficulty,
        @NotNull(message = "El tipo es obligatorio") ChallengeType type,
        @NotNull(message = "El lenguaje es obligatorio") ProgrammingLanguage language,
        String starterCode,
        @NotEmpty(message = "Debe existir al menos un caso de prueba")
        List<@Valid TestCaseRequest> testCases,
        // Rúbrica de evaluación del engine ("introductorio"/"avanzado"), NO el ProfileEntity
        // técnico (lenguaje/sandbox). Opcional: si no viene, PracticalChallengeService la
        // default-ea a "introductorio" al crear.
        String evaluationProfileId
) {
    // Compat: constructor previo a evaluationProfileId, usado por tests existentes.
    public PracticalChallengeRequest(String desafioId, String title, String statement, Difficulty difficulty,
            ChallengeType type, ProgrammingLanguage language, String starterCode,
            List<@Valid TestCaseRequest> testCases) {
        this(desafioId, title, statement, difficulty, type, language, starterCode, testCases, null);
    }

    public record TestCaseRequest(
            @NotBlank(message = "El nombre del caso es obligatorio") String name,
            @NotNull(message = "La entrada debe estar presente") String input,
            @NotNull(message = "La salida esperada debe estar presente") String expectedOutput,
            @NotNull(message = "La visibilidad es obligatoria") TestVisibility visibility
    ) {
    }
}
