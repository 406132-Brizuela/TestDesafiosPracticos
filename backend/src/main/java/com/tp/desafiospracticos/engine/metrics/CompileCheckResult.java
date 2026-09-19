package com.tp.desafiospracticos.engine.metrics;

import java.util.List;

/**
 * Resultado de un chequeo de SOLO compilación (POST /engine/compile): sin tests, sin
 * analisis estatico, sin quality. {@code diagnostics} queda vacio cuando {@code compiles}
 * es true.
 * <p>
 * {@code sandboxAvailable} distingue "el sandbox respondió y el código no compila"
 * ({@code compiles=false}, {@code sandboxAvailable=true}) de "el sandbox no respondió"
 * ({@link #sandboxUnavailable()}) — este segundo caso NO es un error de compilación real,
 * así que un cliente no debería mostrarlo como tal.
 */
public record CompileCheckResult(boolean compiles, List<CompileDiagnostic> diagnostics, boolean sandboxAvailable) {

    public CompileCheckResult(boolean compiles, List<CompileDiagnostic> diagnostics) {
        this(compiles, diagnostics, true);
    }

    public static CompileCheckResult sandboxUnavailable() {
        return new CompileCheckResult(
                false,
                List.of(new CompileDiagnostic(0, "Sandbox no disponible. Intentá de nuevo más tarde.")),
                false
        );
    }
}
