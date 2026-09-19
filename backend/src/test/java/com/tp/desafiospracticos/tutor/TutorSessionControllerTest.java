package com.tp.desafiospracticos.tutor;

import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeExceptionHandler;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TutorSessionController.class)
@ActiveProfiles("local")
@Import(PracticalChallengeExceptionHandler.class)
@TestPropertySource(properties = "app.api.private-path=/api/desafiospracticos")
class TutorSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TutorSessionService service;

    @Test
    void creaLaSesionDelTutorParaElIntento() throws Exception {
        when(service.createForAttempt(eq("attempt-1"), isNull())).thenReturn(new TutorSessionResponse(
                "session-1", "attempt-1", "challenge-1", "ACTIVE"));

        mockMvc.perform(post("/api/desafiospracticos/intentos/attempt-1/tutor/sesion"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void devuelveBadGatewayCuandoLlmNoEstaDisponible() throws Exception {
        when(service.createForAttempt(eq("attempt-1"), isNull()))
                .thenThrow(new TutorServiceUnavailableException("No se pudo conectar con el tutor IA"));

        mockMvc.perform(post("/api/desafiospracticos/intentos/attempt-1/tutor/sesion"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("No se pudo conectar con el tutor IA"));
    }

    @Test
    void validaYEnviaUnMensajeAlTutor() throws Exception {
        TutorMessageRequest request = new TutorMessageRequest("Necesito una pista", "class Main {}");
        when(service.sendMessage(eq("attempt-1"), isNull(), eq(request)))
                .thenReturn(new TutorMessageResponse(
                        "message-1",
                        "session-1",
                        "TUTOR",
                        TutorSessionCatalogText.MOCK_RESPONSE,
                        Instant.parse("2026-09-19T12:00:00Z")
                ));

        mockMvc.perform(post("/api/desafiospracticos/intentos/attempt-1/tutor/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Necesito una pista",
                                  "currentCode": "class Main {}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TUTOR"))
                .andExpect(jsonPath("$.content").value(TutorSessionCatalogText.MOCK_RESPONSE));
    }

    @Test
    void rechazaUnMensajeVacioAntesDeInvocarElServicio() throws Exception {
        mockMvc.perform(post("/api/desafiospracticos/intentos/attempt-1/tutor/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.content").exists());
    }

    private static final class TutorSessionCatalogText {
        private static final String MOCK_RESPONSE =
                "El servicio de tutor IA se encuentra mockeado. Tu mensaje fue recibido, "
                        + "pero todavía no se generan respuestas personalizadas.";
    }
}
