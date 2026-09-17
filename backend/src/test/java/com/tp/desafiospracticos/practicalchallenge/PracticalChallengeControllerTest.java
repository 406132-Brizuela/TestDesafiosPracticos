package com.tp.desafiospracticos.practicalchallenge;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PracticalChallengeController.class)
@Import({SecurityConfig.class, GatewayIdentityFilter.class, PracticalChallengeExceptionHandler.class})
@TestPropertySource(properties = {
        "app.api.public-path=/api/desafiospracticos/public",
        "app.api.private-path=/api/desafiospracticos"
})
class PracticalChallengeControllerTest {

    private static final String BASE_PATH = "/api/desafiospracticos/desafios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PracticalChallengeService service;

    @Test
    void permiteCrearAProfesor() throws Exception {
        PracticalChallengeResponse response = response();
        when(service.create(any(), eq("profesor-1"))).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "profesor-1")
                        .header("X-User-Roles", "PROFESOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("challenge-1"));
    }

    @Test
    void listaSoloLosDesafiosDelProfesor() throws Exception {
        when(service.findAll("profesor-1")).thenReturn(List.of(new PracticalChallengeSummaryResponse(
                "challenge-1", "Sumar dos números", Difficulty.BASICO, Instant.parse("2026-09-17T12:00:00Z"), 2)));

        mockMvc.perform(get(BASE_PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "profesor-1")
                        .header("X-User-Roles", "PROFESOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Sumar dos números"))
                .andExpect(jsonPath("$[0].testCount").value(2));
    }

    @Test
    void permiteConsultarDetalleAAdmin() throws Exception {
        when(service.findById("challenge-1")).thenReturn(response());

        mockMvc.perform(get(BASE_PATH + "/challenge-1")
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases[0].visibility").value("PUBLICO"));
    }

    @Test
    void rechazaCrearAUnRolNoAutorizado() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "alumno-1")
                        .header("X-User-Roles", "ALUMNO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void exigeAutenticacionFueraDelPerfilLocal() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/challenge-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void informaLosCamposInvalidos() throws Exception {
        PracticalChallengeRequest invalid = new PracticalChallengeRequest(
                "desafio-1",
                " ",
                "",
                null,
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.JAVA,
                "",
                List.of()
        );

        mockMvc.perform(post(BASE_PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "profesor-1")
                        .header("X-User-Roles", "PROFESOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.statement").exists())
                .andExpect(jsonPath("$.fields.difficulty").exists())
                .andExpect(jsonPath("$.fields.testCases").exists());

        verifyNoInteractions(service);
    }

    @Test
    void validaNombreYVisibilidadDeCadaCaso() throws Exception {
        PracticalChallengeRequest invalid = new PracticalChallengeRequest(
                "desafio-1",
                "Sumar dos números",
                "Leer dos números y mostrar su suma.",
                Difficulty.BASICO,
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.JAVA,
                "",
                List.of(new PracticalChallengeRequest.TestCaseRequest(" ", "", "", null))
        );

        mockMvc.perform(post(BASE_PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "profesor-1")
                        .header("X-User-Roles", "PROFESOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['testCases[0].name']").exists())
                .andExpect(jsonPath("$.fields['testCases[0].visibility']").exists());

        verifyNoInteractions(service);
    }

    private PracticalChallengeRequest validRequest() {
        return new PracticalChallengeRequest(
                "desafio-1",
                "Sumar dos números",
                "Leer dos números y mostrar su suma.",
                Difficulty.BASICO,
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.JAVA,
                "",
                List.of(new PracticalChallengeRequest.TestCaseRequest(
                        "Caso público", "2 3", "5", TestVisibility.PUBLICO))
        );
    }

    private PracticalChallengeResponse response() {
        return new PracticalChallengeResponse(
                "challenge-1",
                "Sumar dos números",
                "Leer dos números y mostrar su suma.",
                Difficulty.BASICO,
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.JAVA,
                "",
                List.of(new PracticalChallengeResponse.TestCaseResponse(
                        1L, "Caso público", "2 3", "5", TestVisibility.PUBLICO))
        );
    }
}
