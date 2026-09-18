package com.tp.desafiospracticos.motor;

public class MotorChallengeNotFoundException extends RuntimeException {

    public MotorChallengeNotFoundException(String desafioId) {
        super("Motor no contiene el desafioId " + desafioId);
    }
}
