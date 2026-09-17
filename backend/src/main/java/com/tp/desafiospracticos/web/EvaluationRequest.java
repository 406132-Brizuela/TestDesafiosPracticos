package com.tp.desafiospracticos.web;

public record EvaluationRequest(
        String submissionId,
        String challengeId,
        String lenguaje,
        String code,
        String profileId
) {
}
