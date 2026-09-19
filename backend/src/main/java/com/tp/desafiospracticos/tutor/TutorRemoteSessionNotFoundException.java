package com.tp.desafiospracticos.tutor;

public class TutorRemoteSessionNotFoundException extends TutorServiceUnavailableException {

    public TutorRemoteSessionNotFoundException() {
        super("La sesión ya no existe en el tutor IA");
    }
}
