package com.tp.desafiospracticos.tutor;

public class TutorAccessDeniedException extends RuntimeException {

    public TutorAccessDeniedException() {
        super("El intento no pertenece al usuario autenticado");
    }
}
