package com.tp.desafiospracticos.engine.metrics;

import com.tp.desafiospracticos.challenge.TestCase;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PLACEHOLDER: esta implementacion NO ejecuta codigo real.
 * Simula una ejecucion exitosa (compiled = true, todos los casos pasan) unicamente para
 * poder probar el flujo end-to-end sin depender de un sandbox real.
 * <p>
 * Reemplazar por la llamada HTTP real al servicio de sandbox cuando este disponible.
 * <p>
 * Para poder simular distintos escenarios (aprobar / reprobar / cap) desde Postman sin tocar
 * esta clase, {@code EngineServiceImpl} sobreescribe el resultado de este stub a partir del
 * campo opcional {@code EvaluationRequest.simulatedTestsPassed}.
 */
@Component
public class StubSandboxClient implements SandboxClient {

    @Override
    public ExecutionMetrics run(String lenguaje, String code, List<TestCase> tests) {
        List<TestResult> results = tests.stream()
                .map(testCase -> new TestResult(testCase.id(), true, testCase.expected(), testCase.expected()))
                .toList();

        return new ExecutionMetrics(true, tests.size(), tests.size(), 0L, false, results);
    }

    @Override
    public CompileCheckResult compileOnly(String lenguaje, String code) {
        return new CompileCheckResult(true, List.of());
    }
}
