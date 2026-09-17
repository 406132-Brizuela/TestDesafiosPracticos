package com.tp.desafiospracticos.engine.feedback;

import com.tp.desafiospracticos.engine.dimension.ComplexityEvaluator;
import com.tp.desafiospracticos.engine.dimension.CorrectnessEvaluator;
import com.tp.desafiospracticos.engine.dimension.PerformanceEvaluator;
import com.tp.desafiospracticos.engine.dimension.StyleEvaluator;
import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Feedback determinista por plantillas (sin LLM): toma las hasta 3 dimensiones con mayor
 * impacto (weight * (100 - subScore) / 100) entre las que pesan y no sacaron 100, y rellena
 * una plantilla fija por dimension con su evidence. Nunca revela el valor esperado de un test
 * ni la correccion exacta: solo ids de casos fallados y metricas agregadas.
 */
@Component
public class FeedbackGenerator {

    private static final int TOP_N = 3;

    public List<String> generate(List<CorrectionDimension> dimensions) {
        return dimensions.stream()
                .filter(d -> d.weight() > 0)
                .filter(d -> d.subScore() != null && d.subScore() < 100)
                .sorted(Comparator.comparingDouble(this::impacto).reversed())
                .limit(TOP_N)
                .map(this::template)
                .toList();
    }

    private double impacto(CorrectionDimension d) {
        return d.weight() * (100 - d.subScore()) / 100.0;
    }

    @SuppressWarnings("unchecked")
    private String template(CorrectionDimension d) {
        Map<String, Object> evidence = d.evidence();
        return switch (d.dimension()) {
            case CorrectnessEvaluator.DIMENSION_ID -> {
                int testsPassed = (int) evidence.getOrDefault("testsPassed", 0);
                int testsTotal = (int) evidence.getOrDefault("testsTotal", 0);
                List<String> fallados = (List<String>) evidence.getOrDefault("fallados", List.of());
                yield "Pasás %d de %d casos, revisá: %s".formatted(testsPassed, testsTotal, String.join(", ", fallados));
            }
            case PerformanceEvaluator.DIMENSION_ID -> {
                Object tiempoMs = evidence.get("tiempoMs");
                Object limiteMs = evidence.get("limiteMs");
                yield "Tu solución tardó %sms sobre un límite de %sms, buscá reducir el tiempo de ejecución.".formatted(tiempoMs, limiteMs);
            }
            case ComplexityEvaluator.DIMENSION_ID -> {
                int anidamiento = (int) evidence.getOrDefault("anidamientoMaxDetectado", 0);
                List<String> antipatrones = (List<String>) evidence.getOrDefault("antipatrones", List.of());
                if (!antipatrones.isEmpty()) {
                    yield "Tenés un bucle anidado de %d niveles, se puede simplificar con menos anidamiento.".formatted(anidamiento);
                }
                yield "Tu código tiene métodos con demasiada complejidad ciclomática, considerá dividirlos en funciones más chicas.";
            }
            case StyleEvaluator.DIMENSION_ID -> {
                List<String> nombres = (List<String>) evidence.getOrDefault("nombresNoDescriptivos", List.of());
                List<String> metodosLargos = (List<String>) evidence.getOrDefault("metodosLargos", List.of());
                if (!nombres.isEmpty()) {
                    yield "Usá nombres más descriptivos en vez de: %s.".formatted(String.join(", ", nombres));
                }
                yield "Tenés métodos muy largos (%s), probá dividirlos en métodos más chicos.".formatted(String.join(", ", metodosLargos));
            }
            default -> "Revisá la dimensión '" + d.dimension() + "', todavía se puede mejorar.";
        };
    }
}
