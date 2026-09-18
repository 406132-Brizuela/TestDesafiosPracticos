package com.tp.motormock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MotorChallengeController.class)
@Import({MotorChallengeCatalog.class, MotorMockExceptionHandler.class})
class MotorChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devuelveCatalogoDeEjemplo() throws Exception {
        mockMvc.perform(get("/api/motor/desafios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("desafio-suma"))
                .andExpect(jsonPath("$[0].difficulty").value("BASICO"));
    }

    @Test
    void devuelve404ParaIdDesconocido() throws Exception {
        mockMvc.perform(get("/api/motor/desafios/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void simulaElRedirectDeMotor() throws Exception {
        mockMvc.perform(get("/motor/desafios/desafio-suma/autorizar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "http://localhost:4200/desafios/nuevo?desafioId=desafio-suma"));
    }
}
