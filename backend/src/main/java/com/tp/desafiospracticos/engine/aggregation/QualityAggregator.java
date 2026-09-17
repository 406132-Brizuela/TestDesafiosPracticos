package com.tp.desafiospracticos.engine.aggregation;

import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula la quality final aplicando el cap de correctness: si correctness no alcanza
 * el correctnessThreshold del perfil, ninguna otra dimension acredita y la quality queda
 * igualada al subScore de correctness. suggestedVerdict compara contra el approvalThreshold.
 */
@Component
public class QualityAggregator {

    private static final int SCALE = 2;

    public AggregationResult aggregate(List<CorrectionDimension> dimensions, EvaluationProfile profile) {
        int correctnessSubScore = dimensions.stream()
                .filter(dimension -> CorrectnessEvaluator.DIMENSION_ID.equals(dimension.dimension()))
                .findFirst()
                .map(CorrectionDimension::subScore)
                .orElse(0);

        BigDecimal quality = correctnessSubScore < profile.correctnessThreshold()
                ? BigDecimal.valueOf(correctnessSubScore).setScale(SCALE, RoundingMode.HALF_UP)
                : sumaDeContribuciones(dimensions);

        Verdict suggestedVerdict = quality.compareTo(BigDecimal.valueOf(profile.approvalThreshold())) >= 0
                ? Verdict.APPROVED
                : Verdict.NOT_APPROVED;

        return new AggregationResult(quality, suggestedVerdict);
    }

    private BigDecimal sumaDeContribuciones(List<CorrectionDimension> dimensions) {
        BigDecimal suma = dimensions.stream()
                .filter(dimension -> dimension.subScore() != null)
                .map(dimension -> BigDecimal.valueOf(dimension.subScore()).multiply(BigDecimal.valueOf(dimension.weight())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        return suma.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
