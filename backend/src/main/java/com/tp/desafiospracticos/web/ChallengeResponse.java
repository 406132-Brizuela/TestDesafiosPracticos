package com.tp.desafiospracticos.web;

/**
 * Vista publica de un {@code Challenge}: sin los {@code TestCase.expected}, que se resuelven
 * server-side y nunca se exponen al front.
 */
public record ChallengeResponse(String id, String consigna, String lenguaje, String starterCode) {
}
