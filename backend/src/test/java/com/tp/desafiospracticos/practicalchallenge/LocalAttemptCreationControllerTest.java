package com.tp.desafiospracticos.practicalchallenge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalAttemptCreationController.class)
@ActiveProfiles("local")
@Import(PracticalChallengeExceptionHandler.class)
@TestPropertySource(properties = "app.api.private-path=/api/desafiospracticos")
class LocalAttemptCreationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttemptService service;

    @Test
    void iniciaUnIntentoSinAutenticacionSoloEnLocal() throws Exception {
        when(service.start(eq("challenge-1"), isNull())).thenReturn(new AttemptResponse(
                "attempt-1",
                "challenge-1",
                "Sumar dos números",
                Instant.parse("2026-09-17T12:00:00Z"),
                "INICIADO"
        ));

        mockMvc.perform(post("/api/desafiospracticos/intentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicalChallengeId\":\"challenge-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INICIADO"));
    }
}
