package com.tp.llmmock;

import jakarta.validation.constraints.NotBlank;

public record TutorSessionCreateRequest(
        @NotBlank String attemptId,
        @NotBlank String practicalChallengeId
) {
}
