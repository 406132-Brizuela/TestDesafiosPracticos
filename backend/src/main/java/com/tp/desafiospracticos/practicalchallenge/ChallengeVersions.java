package com.tp.desafiospracticos.practicalchallenge;

import java.util.Comparator;
import java.util.List;

/**
 * Resuelve los tests de la version vigente (la de mayor numero) de un desafio.
 * Compartido entre {@link PracticalChallengeService} (vista de profesor) y el
 * adaptador JPA del engine (com.tp.desafiospracticos.challenge.JpaChallengeRepository),
 * para no duplicar el criterio de "version vigente" en dos lugares.
 */
public final class ChallengeVersions {

    private ChallengeVersions() {
    }

    public static List<ChallengeVersionEntity> currentTests(PracticalChallengeEntity challenge) {
        int currentVersion = challenge.getVersions().stream()
                .mapToInt(ChallengeVersionEntity::getVersion)
                .max()
                .orElse(1);
        return challenge.getVersions().stream()
                .filter(version -> version.getVersion() == currentVersion)
                .sorted(Comparator.comparingInt(ChallengeVersionEntity::getTestOrder))
                .toList();
    }
}
