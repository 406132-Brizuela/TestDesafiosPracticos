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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalAttemptDraftController.class)
@ActiveProfiles("local")
@Import(PracticalChallengeExceptionHandler.class)
@TestPropertySource(properties = "app.api.private-path=/api/desafiospracticos")
class LocalAttemptDraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttemptService service;

    @Test
    void guardaElBorradorSinAutenticacionSoloEnLocal() throws Exception {
        when(service.saveDraft(eq("attempt-1"), eq("codigo nuevo"), isNull())).thenReturn(new AttemptDetailResponse(
                "attempt-1",
                "challenge-1",
                "Sumar dos números",
                "Leer dos números y mostrar su suma.",
                "public class Main {}",
                "codigo nuevo",
                "INICIADO",
                Instant.parse("2026-09-17T12:00:00Z")
        ));

        mockMvc.perform(put("/api/desafiospracticos/intentos/attempt-1/borrador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"codigo nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftCode").value("codigo nuevo"));
    }

    @Test
    void devuelve404CuandoElIntentoNoExiste() throws Exception {
        when(service.saveDraft(eq("no-existe"), eq("codigo"), isNull()))
                .thenThrow(new AttemptNotFoundException("no-existe"));

        mockMvc.perform(put("/api/desafiospracticos/intentos/no-existe/borrador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"codigo\"}"))
                .andExpect(status().isNotFound());
    }
}
