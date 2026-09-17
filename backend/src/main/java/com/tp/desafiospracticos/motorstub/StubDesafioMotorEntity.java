package com.tp.desafiospracticos.motorstub;

import com.tp.desafiospracticos.practicalchallenge.Difficulty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla propia del stub: "recuerda" lo que {@link StubMotorDesafioClient}
 * recibió al crear un desafío, para que G05 pueda seguir mostrando
 * title/difficulty sin duplicarlos en su propio modelo de dominio. Temporal:
 * desaparece junto con el resto del paquete {@code motorstub} al integrar
 * Motor real.
 */
@Entity
@Table(name = "stub_motor_desafios")
public class StubDesafioMotorEntity {

    @Id
    @Column(name = "desafio_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    protected StubDesafioMotorEntity() {
    }

    public StubDesafioMotorEntity(String id, String title, Difficulty difficulty) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}
