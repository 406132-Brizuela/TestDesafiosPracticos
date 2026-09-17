package com.tp.desafiospracticos.motorstub;

import com.tp.desafiospracticos.practicalchallenge.Difficulty;

import org.springframework.stereotype.Component;

/**
 * Adaptador temporal de {@link MotorDesafioClient}. Motor (Tema 03) todavía
 * no existe en local, así que el {@code desafioId} llega hoy simulado (en
 * vez de por el redirect de browser real) y este stub simplemente persiste
 * lo recibido (desafioId, title, difficulty) en {@link StubDesafioMotorEntity}
 * para que G05 lo pueda seguir mostrando en listado y detalle.
 *
 * Temporal: a diferencia de otros stubs de este paquete que el día de
 * mañana se reemplazan por un adaptador real, este puerto y su tabla
 * desaparecen por completo al integrar Motor — no hay HTTP client que lo
 * reemplace, porque nunca hubo una llamada saliente real de por medio.
 */
@Component
public class StubMotorDesafioClient implements MotorDesafioClient {

    private final StubDesafioMotorJpaRepository repository;

    public StubMotorDesafioClient(StubDesafioMotorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrarDesafioRecibido(String desafioId, String title, Difficulty difficulty) {
        repository.save(new StubDesafioMotorEntity(desafioId, title, difficulty));
    }
}
