package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guardado del borrador de código del alumno mientras resuelve un intento.
 * Guardar sin pasar por Git es una capacidad de prototipo local, no de
 * producción todavía — mismo criterio que {@link LocalAttemptCreationController}.
 * El almacenamiento real vive en el paquete {@code attemptdraft}, explícita
 * y temporalmente separado del modelo real del intento.
 */
@RestController
@RequestMapping("${app.api.private-path}/intentos")
@Profile("local")
public class LocalAttemptDraftController {

    private final AttemptService service;

    public LocalAttemptDraftController(AttemptService service) {
        this.service = service;
    }

    @PutMapping("/{id}/borrador")
    public AttemptDetailResponse saveDraft(@PathVariable String id,
                                           @Valid @RequestBody AttemptDraftSaveRequest request,
                                           Authentication authentication) {
        return service.saveDraft(
                id,
                request.content(),
                authentication == null ? null : authentication.getName()
        );
    }
}
