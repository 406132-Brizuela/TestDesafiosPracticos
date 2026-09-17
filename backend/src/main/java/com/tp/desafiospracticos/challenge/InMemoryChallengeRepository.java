package com.tp.desafiospracticos.challenge;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio en memoria: siembra un unico desafio "sample" para el MVP.
 * Cuando exista persistencia real, esta clase se reemplaza por una implementacion con JPA.
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
            )
    );

    @Override
    public Challenge findById(String id) {
        if (SAMPLE_CHALLENGE.id().equals(id)) {
            return SAMPLE_CHALLENGE;
        }
        throw new IllegalArgumentException("Desafio no encontrado: " + id);
    }
}
