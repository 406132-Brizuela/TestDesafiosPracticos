package com.tp.desafiospracticos.motor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Combina resolución estricta para detalle/alta y tolerante para listados. */
@Component
public class MotorChallengeResolver {

    public static final String FALLBACK_TITLE = "(sin datos de Motor)";

    private static final Logger log = LoggerFactory.getLogger(MotorChallengeResolver.class);

    private final MotorDesafioClient client;

    public MotorChallengeResolver(MotorDesafioClient client) {
        this.client = client;
    }

    public MotorChallenge resolveOrThrow(String desafioId) {
        return client.findById(desafioId);
    }

    public Map<String, MotorChallenge> resolveBatch(Collection<String> desafioIds) {
        try {
            return client.findAllByIds(desafioIds).stream()
                    .collect(Collectors.toMap(MotorChallenge::id, Function.identity()));
        } catch (RuntimeException exception) {
            log.warn("No se pudo consultar el lote de desafíos en Motor; se usarán fallbacks", exception);
            return Map.of();
        }
    }

    public MotorChallenge resolveFromBatchOrFallback(
            Map<String, MotorChallenge> batch, String desafioId) {
        MotorChallenge data = batch.get(desafioId);
        if (data != null) {
            return data;
        }
        log.warn("Motor no devolvió datos para el desafioId {}; se usa fallback", desafioId);
        return new MotorChallenge(desafioId, FALLBACK_TITLE, null);
    }
}
