package com.tp.motormock;

public record MotorChallenge(String id, String title, Difficulty difficulty) {

    public enum Difficulty {
        BASICO,
        MEDIO,
        AVANZADO
    }
}
