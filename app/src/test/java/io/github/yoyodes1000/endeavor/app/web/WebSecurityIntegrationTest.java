package io.github.yoyodes1000.endeavor.app.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void repondALaMachineLocaleParSonNomOuSonAdresse() throws Exception {
        mvc.perform(get("http://localhost:8080/api/version")).andExpect(status().isOk());
        mvc.perform(get("http://127.0.0.1:8080/api/version")).andExpect(status().isOk());
    }

    @Test
    void refuseUneRequeteAdresseeAUnAutreNom() throws Exception {
        mvc.perform(get("http://attaquant.example:8080/api/version")).andExpect(status().isForbidden());
    }

    @Test
    void poseLesEnTetesDeSecurite() throws Exception {
        mvc.perform(get("/api/version"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void uneErreurNeLaissePasFuirDeDetailInterne() throws Exception {
        mvc.perform(get("/api/inexistant"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
