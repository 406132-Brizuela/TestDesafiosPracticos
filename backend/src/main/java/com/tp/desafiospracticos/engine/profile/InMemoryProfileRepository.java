package com.tp.desafiospracticos.engine.profile;

import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Repositorio en memoria: siembra un unico perfil "default" para el MVP.
 * Cuando exista persistencia real, esta clase se reemplaza por una implementacion con JPA.
 */
@Repository
public class InMemoryProfileRepository implements ProfileRepository {

    private static final EvaluationProfile DEFAULT_PROFILE = new EvaluationProfile(
            "default", 1, 50, Map.of("correctness", 100)
    );

    @Override
    public EvaluationProfile findById(String id) {
        if (DEFAULT_PROFILE.profileId().equals(id)) {
            return DEFAULT_PROFILE;
        }
        throw new IllegalArgumentException("Perfil no encontrado: " + id);
    }
}
