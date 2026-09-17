package com.tp.desafiospracticos.practicalchallenge;

public class PracticalChallengeNotFoundException extends RuntimeException {

    public PracticalChallengeNotFoundException(String id) {
        super("Desafío práctico no encontrado: " + id);
    }
}
