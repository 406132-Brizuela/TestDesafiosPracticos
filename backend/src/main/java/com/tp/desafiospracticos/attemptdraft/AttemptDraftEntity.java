package com.tp.desafiospracticos.attemptdraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Almacenamiento temporal y explícitamente separado del código que el
 * alumno va escribiendo mientras resuelve un intento. NO es parte del
 * modelo real del intento: {@link com.tp.desafiospracticos.practicalchallenge.AttemptEntity}
 * ya tiene {@code repoUrl}/{@code ref} como destino final del código (DT-08,
 * vía integración con GitHub) y esos campos no se tocan acá.
 *
 * <p>Mismo patrón que el paquete {@code motorstub}: vive en un paquete
 * aparte, explícitamente temporal. El día que el código del alumno viva en
 * Git, este paquete se elimina entero — no se convierte en nada.
 */
@Entity
@Table(name = "attempt_drafts")
public class AttemptDraftEntity {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttemptDraftEntity() {
    }

    public AttemptDraftEntity(String id, String content, Instant updatedAt) {
        this.id = id;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public void updateContent(String content, Instant updatedAt) {
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
