package com.tp.desafiospracticos.tutor;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.private-path}/intentos/{attemptId}/tutor")
@PreAuthorize("hasRole('ALUMNO')")
public class TutorSessionController {

    private final TutorSessionService service;

    public TutorSessionController(TutorSessionService service) {
        this.service = service;
    }

    @PostMapping("/sesion")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorSessionResponse create(@PathVariable String attemptId,
                                       Authentication authentication) {
        return service.createForAttempt(attemptId, userId(authentication));
    }

    @PostMapping("/mensajes")
    public TutorMessageResponse sendMessage(@PathVariable String attemptId,
                                            @Valid @RequestBody TutorMessageRequest request,
                                            Authentication authentication) {
        return service.sendMessage(attemptId, userId(authentication), request);
    }

    private String userId(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
