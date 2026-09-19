package com.tp.desafiospracticos.engine.domain;

public enum DimensionState {
    OK,
    /** Sandbox caído: se reintenta cuando esté disponible. subScore null, no cuenta. */
    PENDING_SANDBOX,
    /** No hay analizador para el lenguaje del request. FINAL: no se reintenta. subScore null, no cuenta. */
    NOT_APPLICABLE
}
