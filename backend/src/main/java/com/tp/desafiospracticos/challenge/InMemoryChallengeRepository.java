package com.tp.desafiospracticos.challenge;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio en memoria: siembra un unico desafio "sample". Ya no es el adaptador
 * activo — {@link JpaChallengeRepository} es {@code @Primary} y resuelve desafios
 * reales por challengeId. Esta clase queda registrada como bean secundario (nunca
 * elegido mientras el primario exista) solo por si algun test de contexto Spring
 * necesita un {@code ChallengeRepository} sin base de datos; los tests unitarios de
 * {@code EngineServiceImpl} directamente instancian su propio {@code ChallengeRepository}
 * con una lambda y no dependen de esta clase.
 */
@Repository
public class InMemoryChallengeRepository implements ChallengeRepository {

    private static final Challenge SAMPLE_CHALLENGE = new Challenge(
            "sample",
            "Leé dos enteros por entrada estándar y mostrá su suma.",
            "java",
            """
            import java.util.Scanner;

            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    // TODO: leer dos enteros y mostrar su suma
                }
            }
            """,
            List.of(
                    new TestCase("case1", "3 4", "7"),
                    new TestCase("case2", "10 -2", "8"),
                    new TestCase("case3", "0 0", "0")
            ),
            "introductorio"
    );

    @Override
    public Challenge findById(String id) {
        if (SAMPLE_CHALLENGE.id().equals(id)) {
            return SAMPLE_CHALLENGE;
        }
        throw new IllegalArgumentException("Desafio no encontrado: " + id);
    }
}
