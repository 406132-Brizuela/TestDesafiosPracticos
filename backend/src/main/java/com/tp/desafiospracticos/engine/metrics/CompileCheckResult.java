package com.tp.desafiospracticos.engine.metrics;

import java.util.List;

/**
 * Resultado de un chequeo de SOLO compilación (POST /engine/compile): sin tests, sin
 * analisis estatico, sin quality. {@code diagnostics} queda vacio cuando {@code compiles}
 * es true.
 */
public record CompileCheckResult(boolean compiles, List<CompileDiagnostic> diagnostics) {
}
