package com.tp.desafiospracticos.engine.staticanalysis;

/**
 * Contrato de analisis estatico de codigo fuente. Hoy solo hay implementacion para Java
 * ({@link JavaStaticAnalyzer}); la interfaz queda lista para sumar otros lenguajes.
 */
public interface StaticAnalyzer {

    StaticAnalysisResult analyze(String lenguaje, String code);
}
