package com.tp.llmmock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TutorSessionController.class)
@Import(TutorSessionCatalog.class)
class TutorSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TutorSessionCatalog catalog;

    @Test
    void creaUnaSesionActivaParaElIntento() throws Exception {
        mockMvc.perform(post("/api/llm/tutor/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId": "attempt-1",
                                  "practicalChallengeId": "challenge-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", not(blankOrNullString())))
                .andExpect(jsonPath("$.attemptId").value("attempt-1"))
                .andExpect(jsonPath("$.practicalChallengeId").value("challenge-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rechazaUnaSolicitudSinIdentificadores() throws Exception {
        mockMvc.perform(post("/api/llm/tutor/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reutilizaLaSesionCuandoSeReintentaElMismoIntento() {
        TutorSessionCreateRequest request = new TutorSessionCreateRequest("attempt-2", "challenge-1");

        TutorSession first = catalog.create(request);
        TutorSession repeated = catalog.create(request);

        assertEquals(first.sessionId(), repeated.sessionId());
    }

    @Test
    void guardaElHistorialYDevuelveLaRespuestaMockeada() throws Exception {
        TutorSession session = catalog.create(
                new TutorSessionCreateRequest("attempt-message", "challenge-1"));

        mockMvc.perform(post("/api/llm/tutor/sessions/{sessionId}/messages", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId": "attempt-message",
                                  "content": "¿Me das una pista?",
                                  "context": {
                                    "practicalChallengeId": "challenge-1",
                                    "statement": "Resolver el ejercicio",
                                    "starterCode": "class Main {}",
                                    "currentCode": "class Main { }"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId()))
                .andExpect(jsonPath("$.role").value("TUTOR"))
                .andExpect(jsonPath("$.content").value(TutorSessionCatalog.MOCK_RESPONSE));

        assertEquals(2, catalog.history(session.sessionId()).size());
        assertEquals("STUDENT", catalog.history(session.sessionId()).get(0).role());
        assertEquals("TUTOR", catalog.history(session.sessionId()).get(1).role());
    }

    @Test
    void rechazaUnMensajeParaUnaSesionAjena() throws Exception {
        TutorSession session = catalog.create(
                new TutorSessionCreateRequest("attempt-owner", "challenge-1"));

        mockMvc.perform(post("/api/llm/tutor/sessions/{sessionId}/messages", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId": "otro-attempt",
                                  "content": "Ayuda",
                                  "context": {
                                    "practicalChallengeId": "challenge-1",
                                    "statement": "Resolver el ejercicio",
                                    "starterCode": "",
                                    "currentCode": ""
                                  }
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
