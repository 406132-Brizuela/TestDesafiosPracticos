package com.tp.desafiospracticos.engine.aggregation;

import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.Verdict;
import com.tp.desafiospracticos.engine.profile.EvaluationProfile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calcula la quality final aplicando el cap de correctness: si correctness no alcanza
 * el umbral del perfil, ninguna otra dimension acredita y la quality queda igualada al
 * subScore de correctness.
 */
@Component
public class QualityAggregator {

    public AggregationResult aggregate(List<CorrectionDimension> dimensions, EvaluationProfile profile) {
        int correctnessSubScore = dimensions.stream()
                .filter(dimension -> CorrectnessEvaluator.DIMENSION_ID.equals(dimension.dimension()))
                .findFirst()
                .map(CorrectionDimension::subScore)
                .orElse(0);

        int quality = correctnessSubScore < profile.correctnessThreshold()
                ? correctnessSubScore
                : dimensions.stream().mapToInt(dimension -> (int) Math.round(dimension.contribution())).sum();

        Verdict suggestedVerdict = quality >= profile.correctnessThreshold() ? Verdict.APPROVED : Verdict.NOT_APPROVED;

        return new AggregationResult(quality, suggestedVerdict);
    }
}
