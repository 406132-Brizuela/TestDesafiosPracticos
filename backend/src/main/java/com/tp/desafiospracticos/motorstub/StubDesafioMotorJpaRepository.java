package com.tp.desafiospracticos.motorstub;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Store del stub de Motor. Temporal: desaparece junto con el resto del
 * paquete {@code motorstub} al integrar Motor real.
 */
public interface StubDesafioMotorJpaRepository extends JpaRepository<StubDesafioMotorEntity, String> {
}
