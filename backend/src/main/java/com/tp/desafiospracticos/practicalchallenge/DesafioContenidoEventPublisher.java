package com.tp.desafiospracticos.practicalchallenge;

/**
 * Puerto del evento de dominio "contenido práctico persistido" para un
 * desafío. Se dispara una sola vez, la primera vez que se persiste contenido
 * para un {@code desafioId} (mismo criterio que el contrato con Motor). El
 * bus real (Kafka) lo define Tema 11, que todavía no está disponible.
 */
public interface DesafioContenidoEventPublisher {

    void publicarContenidoPersistido(String desafioId, String contenidoId);
}
