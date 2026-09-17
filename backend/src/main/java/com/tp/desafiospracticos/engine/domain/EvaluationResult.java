package com.tp.desafiospracticos.engine.domain;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationResult(
        String submissionId,
        String profileId,
        int profileVersion,
        String engineVersion,
        EvaluationStatus status,
        BigDecimal quality,
        Verdict suggestedVerdict,
        int approvalThreshold,
        List<CorrectionDimension> dimensions,
        List<String> feedbackAlumno
) {
}
