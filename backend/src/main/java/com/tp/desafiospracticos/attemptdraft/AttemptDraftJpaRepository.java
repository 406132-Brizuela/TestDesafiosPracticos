package com.tp.desafiospracticos.attemptdraft;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Store del borrador temporal del código del alumno. Temporal: desaparece
 * junto con el resto del paquete {@code attemptdraft} el día que el código
 * del alumno pase a vivir en Git.
 */
public interface AttemptDraftJpaRepository extends JpaRepository<AttemptDraftEntity, String> {
}
