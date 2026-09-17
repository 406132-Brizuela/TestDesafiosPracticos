package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.config.GatewayIdentityFilter;
import com.tp.desafiospracticos.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttemptQueryController.class)
@Import({SecurityConfig.class, GatewayIdentityFilter.class, PracticalChallengeExceptionHandler.class})
@TestPropertySource(properties = {
        "app.api.public-path=/api/desafiospracticos/public",
        "app.api.private-path=/api/desafiospracticos"
})
class AttemptQueryControllerTest {

    private static final String PATH = "/api/desafiospracticos/intentos";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttemptService service;

    @Test
    void permiteListarIntentosAlUsuarioAutenticado() throws Exception {
        when(service.findAll("alumno-1")).thenReturn(List.of(new AttemptResponse(
                "attempt-1",
                "challenge-1",
                "Sumar dos números",
                Instant.parse("2026-09-17T12:00:00Z"),
                "INICIADO"
        )));

        mockMvc.perform(get(PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "alumno-1")
                        .header("X-User-Roles", "ALUMNO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INICIADO"));
    }

    @Test
    void noExponeInicioDeIntentosFueraDeLocal() throws Exception {
        mockMvc.perform(post(PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "alumno-1")
                        .header("X-User-Roles", "ALUMNO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicalChallengeId\":\"challenge-1\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
