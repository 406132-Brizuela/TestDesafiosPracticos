package com.tp.desafiospracticos.engine.profile;

import java.util.Map;

public record EvaluationProfile(
        String profileId,
        int version,
        int approvalThreshold,
        int correctnessThreshold,
        Map<String, PesoDim> dimensiones
) {

    public int weightOf(String dimensionId) {
        PesoDim peso = dimensiones.get(dimensionId);
        return peso == null ? 0 : peso.weight();
    }

    public Map<String, Object> paramsOf(String dimensionId) {
        PesoDim peso = dimensiones.get(dimensionId);
        return peso == null || peso.params() == null ? Map.of() : peso.params();
    }
}
