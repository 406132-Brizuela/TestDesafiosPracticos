package com.tp.llmmock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TutorMessageCreateRequest(
        @NotBlank String attemptId,
        @NotBlank @Size(max = 2000) String content,
        @Valid @NotNull TutorContext context
) {
    public record TutorContext(
            @NotBlank String practicalChallengeId,
            @NotBlank String statement,
            @NotNull String starterCode,
            @NotNull String currentCode
    ) {
    }
}
