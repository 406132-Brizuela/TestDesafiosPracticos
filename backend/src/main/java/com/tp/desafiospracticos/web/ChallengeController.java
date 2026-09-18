package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import com.tp.desafiospracticos.practicalchallenge.ChallengeVersionEntity;
import com.tp.desafiospracticos.practicalchallenge.ChallengeVersions;
import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeJpaRepository;
import com.tp.desafiospracticos.practicalchallenge.TestVisibility;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vista del desafío para quien lo RESUELVE (sin expected, ver {@link ChallengeResponse}).
 * La vista completa para el profesor (incluye expectedOutput de todos los tests) sigue
 * viviendo en {@code practicalchallenge.PracticalChallengeController}, gateada a
 * ADMIN/PROFESOR.
 */
@RestController
@RequestMapping("/challenges")
public class ChallengeController {

    private final ChallengeRepository challengeRepository;
    private final PracticalChallengeJpaRepository practicalChallengeRepository;

    public ChallengeController(ChallengeRepository challengeRepository,
                                PracticalChallengeJpaRepository practicalChallengeRepository) {
        this.challengeRepository = challengeRepository;
        this.practicalChallengeRepository = practicalChallengeRepository;
    }

    // @Transactional: publicTestsOf navega asociaciones LAZY de PracticalChallengeEntity
    // (versions -> test) fuera del adaptador del engine, así que necesita su propia
    // sesión abierta (ver JpaChallengeRepository, mismo motivo).
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ChallengeResponse getChallenge(@PathVariable String id) {
        Challenge challenge = challengeRepository.findById(id);
        return new ChallengeResponse(
                challenge.id(), challenge.consigna(), challenge.lenguaje(), challenge.starterCode(),
                publicTestsOf(id));
    }

    private List<ChallengeResponse.PublicTestCase> publicTestsOf(String id) {
        return practicalChallengeRepository.findById(id)
                .map(ChallengeVersions::currentTests)
                .orElse(List.of())
                .stream()
                .map(ChallengeVersionEntity::getTest)
                .filter(test -> test.getVisibility() == TestVisibility.PUBLICO)
                .map(test -> new ChallengeResponse.PublicTestCase(test.getName(), test.getInput()))
                .toList();
    }
}
