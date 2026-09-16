package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.challenge.TestCase;

import java.util.List;

/**
 * @param simulatedTestsPassed Campo opcional solo para el MVP: como {@code StubSandboxClient}
 *                              simula que compilan y pasan todos los casos, este valor permite
 *                              forzar cuantos casos pasan para poder probar los distintos caminos
 *                              (aprobar / reprobar / cap) desde Postman. Ignorar/eliminar cuando
 *                              se conecte el sandbox real.
 */
public record EvaluationRequest(
        String submissionId,
        String lenguaje,
        String code,
        List<TestCase> tests,
        String profileId,
        Integer simulatedTestsPassed
) {
}
