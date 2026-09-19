package com.tp.desafiospracticos.engine.metrics;

/**
 * {@code path} es el path relativo del archivo (dentro del submission) al que corresponde el
 * diagnostic; null para mensajes que no vienen de una linea de un archivo puntual (timeout,
 * error de I/O, etc.) — ver el constructor de 2 args, que preserva ese uso previo a
 * soportar multi-archivo.
 */
public record CompileDiagnostic(String path, int line, String message) {

    public CompileDiagnostic(int line, String message) {
        this(null, line, message);
    }
}
