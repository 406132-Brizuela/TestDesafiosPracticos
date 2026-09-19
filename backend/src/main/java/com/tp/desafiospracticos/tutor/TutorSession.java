package com.tp.desafiospracticos.tutor;

public record TutorSession(
        String sessionId,
        String attemptId,
        String practicalChallengeId,
        String status
) {
}
