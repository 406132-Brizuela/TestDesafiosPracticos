package com.tp.desafiospracticos.challenge;

import java.util.List;

public record Challenge(
        String id,
        String consigna,
        String lenguaje,
        String starterCode,
        List<TestCase> tests
) {
}
