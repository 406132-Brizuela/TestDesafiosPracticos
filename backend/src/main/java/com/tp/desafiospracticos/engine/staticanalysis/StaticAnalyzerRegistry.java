package com.tp.desafiospracticos.engine.staticanalysis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Selecciona el {@link StaticAnalyzer} registrado para el lenguaje del request. Si ninguno
 * lo soporta, NO hay fallback silencioso a "sin problemas": el llamador (EngineServiceImpl)
 * marca las dimensiones estaticas NOT_APPLICABLE en vez de inventar un analisis que no corrio.
 */
@Component
public class StaticAnalyzerRegistry {

    private final List<StaticAnalyzer> analyzers;

    public StaticAnalyzerRegistry(List<StaticAnalyzer> analyzers) {
        this.analyzers = analyzers;
    }

    public Optional<StaticAnalyzer> forLanguage(String lenguaje) {
        return analyzers.stream().filter(analyzer -> analyzer.supports(lenguaje)).findFirst();
    }
}
