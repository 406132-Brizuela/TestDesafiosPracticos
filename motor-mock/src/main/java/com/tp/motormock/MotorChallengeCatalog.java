package com.tp.motormock;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MotorChallengeCatalog {

    private final Map<String, MotorChallenge> challenges;

    public MotorChallengeCatalog() {
        Map<String, MotorChallenge> data = new LinkedHashMap<>();
        add(data, new MotorChallenge(
                "desafio-suma", "Sumar dos números", MotorChallenge.Difficulty.BASICO));
        add(data, new MotorChallenge(
                "desafio-palindromo", "Detectar un palíndromo", MotorChallenge.Difficulty.MEDIO));
        add(data, new MotorChallenge(
                "desafio-ordenamiento", "Ordenar una lista", MotorChallenge.Difficulty.AVANZADO));
        this.challenges = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public List<MotorChallenge> findAll() {
        return challenges.values().stream().toList();
    }

    public MotorChallenge findById(String id) {
        MotorChallenge challenge = challenges.get(id);
        if (challenge == null) {
            throw new MotorChallengeNotFoundException(id);
        }
        return challenge;
    }

    public List<MotorChallenge> findAllByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return findAll();
        }
        return ids.stream().distinct().map(challenges::get).filter(java.util.Objects::nonNull).toList();
    }

    private void add(Map<String, MotorChallenge> data, MotorChallenge challenge) {
        data.put(challenge.id(), challenge);
    }
}
