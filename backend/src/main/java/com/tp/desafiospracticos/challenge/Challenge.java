package com.tp.desafiospracticos.challenge;

import java.util.List;

public record Challenge(
        String id,
        String consigna,
        String lenguaje,
        String starterCode,
        List<TestCase> tests,
        // Rúbrica de evaluación del engine ("introductorio"/"avanzado") — dato del desafío.
        // Puede venir null (desafío sin evaluationProfileId propio); el engine resuelve el
        // default ahí, no acá.
        String evaluationProfileId
) {
}
