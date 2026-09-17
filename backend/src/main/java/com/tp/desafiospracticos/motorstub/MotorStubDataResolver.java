package com.tp.desafiospracticos.motorstub;

import com.tp.desafiospracticos.practicalchallenge.Difficulty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Punto único para resolver datos de Motor (title/difficulty) contra
 * {@link StubDesafioMotorJpaRepository}, usado por {@code PracticalChallengeService}
 * y {@code AttemptService} para evitar duplicar la lógica de resolución.
 *
 * <p>Expone dos formas de resolver, según el flujo:
 * <ul>
 *   <li>{@link #resolveOrThrow(String)}: para creación/consulta individual
 *   (p. ej. {@code create}, {@code GET /desafios/{id}}), donde una fila
 *   faltante del stub es un error real que tiene sentido informar.</li>
 *   <li>{@link #resolveBatch(Collection)} + {@link #resolveFromBatchOrFallback}:
 *   para listados, donde se resuelve todo en una sola query
 *   ({@code findAllById}) y una fila faltante no debe tumbar el listado
 *   entero — se loguea un WARN y se degrada esa fila puntual con un
 *   fallback.</li>
 * </ul>
 *
 * Temporal: vive junto al resto del paquete {@code motorstub} y desaparece
 * cuando se integra Motor real.
 */
@Component
public class MotorStubDataResolver {

    public static final String FALLBACK_TITLE = "(sin datos de Motor)";
    public static final Difficulty FALLBACK_DIFFICULTY = null;

    private static final Logger log = LoggerFactory.getLogger(MotorStubDataResolver.class);

    private final StubDesafioMotorJpaRepository repository;

    public MotorStubDataResolver(StubDesafioMotorJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Resuelve un único {@code desafioId}. Propaga {@link IllegalStateException}
     * si no hay datos de Motor para ese id — apto solo para flujos de
     * creación/consulta individual, donde informar el error puntual es
     * correcto.
     */
    public StubDesafioMotorEntity resolveOrThrow(String desafioId) {
        return repository.findById(desafioId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontraron datos de Motor para el desafioId " + desafioId));
    }

    /**
     * Resuelve en lote los datos de Motor para un conjunto de
     * {@code desafioId}, en una sola consulta ({@code findAllById}), para
     * evitar N+1 al construir listados.
     */
    public Map<String, StubDesafioMotorEntity> resolveBatch(Collection<String> desafioIds) {
        return repository.findAllById(desafioIds).stream()
                .collect(Collectors.toMap(StubDesafioMotorEntity::getId, Function.identity()));
    }

    /**
     * Busca {@code desafioId} dentro de un batch ya resuelto con
     * {@link #resolveBatch(Collection)}. Si falta, loguea un WARN y devuelve
     * un fallback en vez de propagar una excepción, para que una sola fila
     * con datos faltantes no tumbe el listado completo.
     */
    public StubDesafioMotorEntity resolveFromBatchOrFallback(
            Map<String, StubDesafioMotorEntity> batch, String desafioId) {
        StubDesafioMotorEntity data = batch.get(desafioId);
        if (data != null) {
            return data;
        }
        log.warn("No se encontraron datos de Motor para el desafioId {}; se usa un valor de fallback "
                + "en el listado en vez de descartar la fila", desafioId);
        return new StubDesafioMotorEntity(desafioId, FALLBACK_TITLE, FALLBACK_DIFFICULTY);
    }
}
