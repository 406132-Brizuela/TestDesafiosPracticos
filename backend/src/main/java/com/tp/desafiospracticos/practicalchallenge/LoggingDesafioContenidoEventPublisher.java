package com.tp.desafiospracticos.practicalchallenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador stub de {@link DesafioContenidoEventPublisher}: loguea el evento
 * en vez de publicarlo en un bus real. Kafka lo define Tema 11, todavía no
 * está disponible.
 */
@Component
public class LoggingDesafioContenidoEventPublisher implements DesafioContenidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDesafioContenidoEventPublisher.class);

    @Override
    public void publicarContenidoPersistido(String desafioId, String contenidoId) {
        log.info("ContenidoPracticoPersistido desafioId={} contenidoId={}", desafioId, contenidoId);
    }
}
