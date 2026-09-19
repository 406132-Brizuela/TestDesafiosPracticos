package com.tp.desafiospracticos.engine;

import java.util.List;

/**
 * Un archivo fuente identificado por su path relativo (incluye subcarpetas de package,
 * ej. "com/foo/Main.java"). Unidad compartida entre el sandbox y el analisis estatico para
 * soportar submissions multi-archivo.
 */
public record SourceFile(String path, String content) {

    /**
     * Normaliza la entrada de un request: si vienen {@code files} explicitos se usan tal cual;
     * si no, {@code code} se trata como el unico "Main.java" (comportamiento previo a
     * soportar multi-archivo, intacto).
     */
    public static List<SourceFile> resolve(String code, List<SourceFile> files) {
        if (files != null && !files.isEmpty()) {
            return files;
        }
        return List.of(new SourceFile("Main.java", code));
    }
}
