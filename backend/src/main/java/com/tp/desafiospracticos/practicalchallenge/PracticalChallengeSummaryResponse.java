package com.tp.desafiospracticos.practicalchallenge;

import java.time.Instant;

public record PracticalChallengeSummaryResponse(
        String id,
        String title,
        Difficulty difficulty,
        Instant creationDatetime,
        int testCount
) {
}
