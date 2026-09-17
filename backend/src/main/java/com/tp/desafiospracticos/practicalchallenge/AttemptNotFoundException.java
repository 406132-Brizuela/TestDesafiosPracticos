package com.tp.desafiospracticos.practicalchallenge;

public class AttemptNotFoundException extends RuntimeException {

    public AttemptNotFoundException(String id) {
        super("Intento no encontrado: " + id);
    }
}
