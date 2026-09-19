package com.tp.desafiospracticos.engine.staticanalysis;

import com.tp.desafiospracticos.engine.SourceFile;

import java.util.List;

/**
 * Contrato de analisis estatico de codigo fuente. Hoy solo hay implementacion para Java
 * ({@link JavaStaticAnalyzer}); {@link StaticAnalyzerRegistry} selecciona la instancia
 * correcta por {@code lenguaje} vía {@link #supports}.
 */
public interface StaticAnalyzer {

    boolean supports(String lenguaje);

    StaticAnalysisResult analyze(String lenguaje, List<SourceFile> files);

    /** Compat: submission de un solo archivo, tratado como "Main.java". */
    default StaticAnalysisResult analyze(String lenguaje, String code) {
        return analyze(lenguaje, List.of(new SourceFile("Main.java", code)));
    }
}
