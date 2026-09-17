package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.challenge.Challenge;
import com.tp.desafiospracticos.challenge.ChallengeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/challenges")
public class ChallengeController {

    private final ChallengeRepository challengeRepository;

    public ChallengeController(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    @GetMapping("/{id}")
    public ChallengeResponse getChallenge(@PathVariable String id) {
        Challenge challenge = challengeRepository.findById(id);
        return new ChallengeResponse(challenge.id(), challenge.consigna(), challenge.lenguaje(), challenge.starterCode());
    }
}
