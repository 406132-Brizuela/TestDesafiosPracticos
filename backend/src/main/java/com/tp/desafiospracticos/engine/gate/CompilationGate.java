package com.tp.desafiospracticos.engine.gate;

import com.tp.desafiospracticos.engine.metrics.ExecutionMetrics;
import org.springframework.stereotype.Component;

/**
 * Si la entrega no compila, ninguna dimension se calcula: el flujo corta aca con
 * status NO_COMPILE.
 */
@Component
public class CompilationGate {

    public boolean passes(ExecutionMetrics metrics) {
        return metrics.compiled();
    }
}
