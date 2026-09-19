package com.tp.desafiospracticos.tutor;

public class TutorServiceUnavailableException extends RuntimeException {

    public TutorServiceUnavailableException(String message) {
        super(message);
    }

    public TutorServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
