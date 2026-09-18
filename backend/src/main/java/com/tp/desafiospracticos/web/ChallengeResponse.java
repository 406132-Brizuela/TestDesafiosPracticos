package com.tp.desafiospracticos.web;

import java.util.List;

/**
 * Vista publica de un {@code Challenge}: sin los {@code TestCase.expected}, que se resuelven
 * server-side y nunca se exponen al front. Incluye, a lo sumo, el input de los tests
 * PUBLICO (nunca PRIVADO ni su expected) para que el alumno vea ejemplos al resolver.
 */
public record ChallengeResponse(
        String id,
        String consigna,
        String lenguaje,
        String starterCode,
        List<PublicTestCase> testsPublicos
) {
    public record PublicTestCase(String name, String input) {
    }
}
