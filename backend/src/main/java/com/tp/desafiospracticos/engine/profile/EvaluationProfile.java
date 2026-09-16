package com.tp.desafiospracticos.engine.profile;

import java.util.Map;

public record EvaluationProfile(
        String profileId,
        int version,
        int correctnessThreshold,
        Map<String, Integer> weights
) {
}
