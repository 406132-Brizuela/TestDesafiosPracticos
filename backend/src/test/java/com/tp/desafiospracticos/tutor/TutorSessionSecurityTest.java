package com.tp.desafiospracticos.tutor;

import com.tp.desafiospracticos.config.GatewayIdentityFilter;
import com.tp.desafiospracticos.config.SecurityConfig;
import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TutorSessionController.class)
@Import({SecurityConfig.class, GatewayIdentityFilter.class, PracticalChallengeExceptionHandler.class})
@TestPropertySource(properties = {
        "app.api.public-path=/api/desafiospracticos/public",
        "app.api.private-path=/api/desafiospracticos"
})
class TutorSessionSecurityTest {

    private static final String PATH =
            "/api/desafiospracticos/intentos/attempt-1/tutor/sesion";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TutorSessionService service;

    @Test
    void permiteCrearLaSesionAlAlumnoDuenio() throws Exception {
        when(service.createForAttempt(eq("attempt-1"), eq("student-1")))
                .thenReturn(new TutorSessionResponse(
                        "session-1", "attempt-1", "challenge-1", "ACTIVE"));

        mockMvc.perform(post(PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Roles", "ALUMNO"))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaUsuariosSinRolAlumno() throws Exception {
        mockMvc.perform(post(PATH)
                        .header("X-Principal-Type", "user")
                        .header("X-User-Id", "teacher-1")
                        .header("X-User-Roles", "PROFESOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaPeticionesSinIdentidad() throws Exception {
        mockMvc.perform(post(PATH))
                .andExpect(status().isUnauthorized());
    }
}
