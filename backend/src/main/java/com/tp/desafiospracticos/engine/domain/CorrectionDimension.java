package com.tp.desafiospracticos.engine.domain;

import java.util.Map;

public record CorrectionDimension(
        String dimension,
        Integer subScore,
        int weight,
        double contribution,
        DimensionSource source,
        Map<String, Object> evidence
) {
}
