package com.tp.desafiospracticos.web;

import jakarta.servlet.http.HttpServletRequest;
import com.tp.desafiospracticos.config.IdentityHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador placeholder de la convención (adaptado del ejemplo):
 *   ${app.api.public-path}/ping       -> sin token (prueba anti-spoofing)
 *   ${app.api.private-path}/quien-soy -> cualquier autenticado
 *   ${app.api.private-path}/interno   -> rol MS
 * REGLA DURA: placeholders ${app.api.*}, NUNCA literales "/api/...".
 */
@RestController
public class DesafiosController {

    @GetMapping("${app.api.public-path}/ping")
    public Map<String, Object> ping(HttpServletRequest req) {
        return respuesta(req, null);
    }

    @GetMapping("${app.api.private-path}/quien-soy")
    public Map<String, Object> quienSoy(HttpServletRequest req, Authentication auth) {
        return respuesta(req, auth);
    }

    @GetMapping("${app.api.private-path}/interno")
    @PreAuthorize("hasRole('MS')")
    public Map<String, Object> interno(HttpServletRequest req, Authentication auth) {
        return respuesta(req, auth);
    }

    private Map<String, Object> respuesta(HttpServletRequest req, Authentication auth) {
        Map<String, String> headers = new LinkedHashMap<>();
        List.of(IdentityHeaders.PRINCIPAL_TYPE, IdentityHeaders.USER_ID,
                IdentityHeaders.USER_ROLES, IdentityHeaders.SERVICE_ID,
                IdentityHeaders.SERVICE_SCOPES, IdentityHeaders.REQUEST_ID)
                .forEach(h -> headers.put(h, req.getHeader(h)));
        headers.put("Authorization", req.getHeader("Authorization") == null
                ? null : "[presente, no se expone]");

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("servicio", "desafiospracticos-service");
        r.put("path", req.getRequestURI());
        r.put("headersRecibidos", headers);
        r.put("principal", auth == null ? null : auth.getName());
        r.put("authorities", auth == null ? List.of()
                : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return r;
    }
}
