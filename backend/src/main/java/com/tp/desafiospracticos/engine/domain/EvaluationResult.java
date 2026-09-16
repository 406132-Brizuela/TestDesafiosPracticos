package com.tp.desafiospracticos.engine.domain;

import java.util.List;

public record EvaluationResult(
        String submissionId,
        String profileId,
        int profileVersion,
        EvaluationStatus status,
        Integer quality,
        Verdict suggestedVerdict,
        int approvalThreshold,
        List<CorrectionDimension> dimensions
) {
}
