package com.tp.desafiospracticos.tutor;

public record TutorSessionResponse(
        String sessionId,
        String attemptId,
        String practicalChallengeId,
        String status
) {
}
