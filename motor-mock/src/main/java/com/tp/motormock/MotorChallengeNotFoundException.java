package com.tp.motormock;

public class MotorChallengeNotFoundException extends RuntimeException {

    public MotorChallengeNotFoundException(String id) {
        super("Desafío de Motor no encontrado: " + id);
    }
}
