package com.tp.desafiospracticos.practicalchallenge;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("${app.api.private-path}/desafios")
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
public class PracticalChallengeController {

    private final PracticalChallengeService service;

    public PracticalChallengeController(PracticalChallengeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PracticalChallengeResponse> create(
            @Valid @RequestBody PracticalChallengeRequest request,
            Authentication authentication) {
        PracticalChallengeResponse response = service.create(request, principalName(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public List<PracticalChallengeSummaryResponse> findAll(Authentication authentication) {
        return service.findAll(principalName(authentication));
    }

    @GetMapping("/{id}")
    public PracticalChallengeResponse findById(@PathVariable String id) {
        return service.findById(id);
    }

    private String principalName(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
