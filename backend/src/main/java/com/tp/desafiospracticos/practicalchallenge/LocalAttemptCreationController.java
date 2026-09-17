package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.private-path}/intentos")
@Profile("local")
public class LocalAttemptCreationController {

    private final AttemptService service;

    public LocalAttemptCreationController(AttemptService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse start(@Valid @RequestBody AttemptCreateRequest request,
                                 Authentication authentication) {
        return service.start(
                request.practicalChallengeId(),
                authentication == null ? null : authentication.getName()
        );
    }
}
