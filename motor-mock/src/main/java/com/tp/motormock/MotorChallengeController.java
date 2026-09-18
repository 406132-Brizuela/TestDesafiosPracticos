package com.tp.motormock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@RestController
public class MotorChallengeController {

    private final MotorChallengeCatalog catalog;
    private final String frontendUrl;

    public MotorChallengeController(MotorChallengeCatalog catalog,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.catalog = catalog;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/api/motor/desafios")
    public List<MotorChallenge> findAll(@RequestParam(required = false) String ids) {
        List<String> requestedIds = ids == null || ids.isBlank()
                ? List.of()
                : Arrays.stream(ids.split(",")).map(String::trim).filter(id -> !id.isEmpty()).toList();
        return catalog.findAllByIds(requestedIds);
    }

    @GetMapping("/api/motor/desafios/{id}")
    public MotorChallenge findById(@PathVariable String id) {
        return catalog.findById(id);
    }

    /** Simula el redirect de browser que Motor realiza después de crear/seleccionar un desafío. */
    @GetMapping("/motor/desafios/{id}/autorizar")
    public RedirectView redirectToAuthoring(@PathVariable String id) {
        catalog.findById(id);
        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/desafios/nuevo")
                .queryParam("desafioId", id)
                .build()
                .toUriString();
        return new RedirectView(target);
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        StringBuilder html = new StringBuilder("<h1>Motor de desafíos — mock</h1>")
                .append("<p>Elegí un ejemplo para abrir su contenido práctico:</p><ul>");
        catalog.findAll().forEach(challenge -> html.append("<li><a href=\"/motor/desafios/")
                .append(challenge.id()).append("/autorizar\">")
                .append(challenge.title()).append(" — ").append(challenge.difficulty())
                .append("</a></li>"));
        return html.append("</ul>").toString();
    }
}
