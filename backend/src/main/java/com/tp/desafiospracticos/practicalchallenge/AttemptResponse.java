package com.tp.desafiospracticos.practicalchallenge;

import java.time.Instant;

public record AttemptResponse(
        String id,
        String practicalChallengeId,
        String challengeTitle,
        Instant creationDatetime,
        String status
) {
}
