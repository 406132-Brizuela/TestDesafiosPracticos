package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.engine.SourceFile;

import java.util.List;

public record CompileCheckRequest(
        String lenguaje,
        String code,
        // Opcional: submission multi-archivo. Si viene, se usa en vez de `code`.
        List<SourceFile> files
) {

    public CompileCheckRequest(String lenguaje, String code) {
        this(lenguaje, code, null);
    }
}
