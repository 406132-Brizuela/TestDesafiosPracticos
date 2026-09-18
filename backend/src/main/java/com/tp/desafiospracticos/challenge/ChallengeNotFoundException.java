package com.tp.desafiospracticos.challenge;

public class ChallengeNotFoundException extends RuntimeException {

    public ChallengeNotFoundException(String challengeId) {
        super("Desafio no encontrado: " + challengeId);
    }
}
