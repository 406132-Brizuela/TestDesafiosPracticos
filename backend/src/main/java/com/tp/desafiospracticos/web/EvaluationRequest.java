package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.engine.SourceFile;

import java.util.List;

public record EvaluationRequest(
        String submissionId,
        String challengeId,
        String lenguaje,
        String code,
        String profileId,
        // Opcional: submission multi-archivo. Si viene, se usa en vez de `code` (ver
        // SourceFile.resolve). Null/vacio conserva el comportamiento de un solo archivo.
        List<SourceFile> files
) {

    public EvaluationRequest(String submissionId, String challengeId, String lenguaje, String code, String profileId) {
        this(submissionId, challengeId, lenguaje, code, profileId, null);
    }
}
