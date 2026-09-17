package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.private-path}/intentos")
@PreAuthorize("isAuthenticated()")
public class AttemptQueryController {

    private final AttemptService service;

    public AttemptQueryController(AttemptService service) {
        this.service = service;
    }

    @GetMapping
    public List<AttemptResponse> findAll(Authentication authentication) {
        return service.findAll(authentication == null ? null : authentication.getName());
    }
}
