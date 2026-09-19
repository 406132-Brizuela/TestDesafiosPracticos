package com.tp.llmmock;

import java.time.Instant;

public record TutorSession(
        String sessionId,
        String attemptId,
        String practicalChallengeId,
        String status,
        Instant createdAt
) {
}
