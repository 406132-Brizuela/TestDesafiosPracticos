package com.tp.desafiospracticos.motor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogo de solo lectura que el frontend consume sin conocer la URL interna de Motor. */
@RestController
@RequestMapping("${app.api.private-path}/motor/desafios")
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
public class MotorCatalogController {

    private final MotorDesafioClient client;

    public MotorCatalogController(MotorDesafioClient client) {
        this.client = client;
    }

    @GetMapping
    public List<MotorChallenge> findAll() {
        return client.findAll();
    }

    @GetMapping("/{id}")
    public MotorChallenge findById(@PathVariable String id) {
        return client.findById(id);
    }
}
