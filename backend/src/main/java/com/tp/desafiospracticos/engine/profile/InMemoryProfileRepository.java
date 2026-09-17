package com.tp.desafiospracticos.engine.profile;

import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Repositorio en memoria: siembra dos perfiles de rubrica.
 * Cuando exista persistencia real, esta clase se reemplaza por una implementacion con JPA.
 */
@Repository
public class InMemoryProfileRepository implements ProfileRepository {

    private static final Map<String, EvaluationProfile> PROFILES = Map.of(
            "introductorio", new EvaluationProfile(
                    "introductorio", 1, 50, 40,
                    Map.of(
                            "correctness", new PesoDim(80, Map.of()),
                            "performance", new PesoDim(0, Map.of("limiteMs", 3000)),
                            "complexity", new PesoDim(0, Map.of("anidamientoMax", 5, "complejidadMax", 20)),
                            "style", new PesoDim(20, Map.of())
                    )
            ),
            "avanzado", new EvaluationProfile(
                    "avanzado", 1, 50, 50,
                    Map.of(
                            "correctness", new PesoDim(40, Map.of()),
                            "performance", new PesoDim(15, Map.of("limiteMs", 1500)),
                            "complexity", new PesoDim(30, Map.of("anidamientoMax", 2, "complejidadMax", 10)),
                            "style", new PesoDim(15, Map.of())
                    )
            )
    );

    @Override
    public EvaluationProfile findById(String id) {
        EvaluationProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Perfil no encontrado: " + id);
        }
        return profile;
    }
}
