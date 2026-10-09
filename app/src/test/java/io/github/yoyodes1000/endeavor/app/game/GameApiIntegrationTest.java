package io.github.yoyodes1000.endeavor.app.game;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * L'API de partie de bout en bout, à travers HTTP : le contexte Spring complet (matériel
 * chargé depuis le jar, format JSON des coups, traduction des erreurs).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GameApiIntegrationTest {

    /** Garde-fou : une partie réelle tient en bien moins de coups humains que cela. */
    private static final int MAX_HUMAN_MOVES = 5_000;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode startGame(int opponents, long seed) throws Exception {
        String body = postJson("/api/game", new NewGameRequest(1, opponents, seed))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private ObjectNode move(JsonNode view, JsonNode action) {
        ObjectNode move = json.createObjectNode();
        move.put("moveNumber", view.get("moveNumber").asInt());
        move.set("action", action);
        return move;
    }

    @Test
    void listeLesMissionsJouables() throws Exception {
        mvc.perform(get("/api/missions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].number", contains(1, 2, 4, 7, 8, 9, 10)))
                .andExpect(jsonPath("$[0].allGoalsScored").value(true));
    }

    @Test
    void lanceUnePartieEtRendLaMainAuJoueurHumain() throws Exception {
        postJson("/api/game", new NewGameRequest(1, 2, 2026L))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/game"))
                .andExpect(jsonPath("$.mission").value(1))
                .andExpect(jsonPath("$.playerCount").value(3))
                .andExpect(jsonPath("$.phase").value("PREPARATION"))
                .andExpect(jsonPath("$.yourTurn").value(true))
                .andExpect(jsonPath("$.legalActions[0].type").value("Recruter"))
                .andExpect(jsonPath("$.seed").doesNotExist());

        mvc.perform(get("/api/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mission").value(1));
    }

    @Test
    void joueUnePartieEntiereAtraversLApi() throws Exception {
        JsonNode view = startGame(3, 7);
        mvc.perform(get("/api/game/result")).andExpect(status().isConflict());

        int moves = 0;
        while (!view.get("phase").asText().equals("FINISHED")) {
            String body = postJson("/api/game/moves", move(view, view.get("legalActions").get(0)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            view = json.readTree(body);
            if (++moves > MAX_HUMAN_MOVES) {
                throw new AssertionError("la partie ne se termine pas");
            }
        }

        mvc.perform(get("/api/game/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scores", hasSize(4)))
                .andExpect(jsonPath("$.winners").isArray())
                .andExpect(jsonPath("$.complete").isBoolean());
    }

    @Test
    void refuseUnCoupChoisiSurUnEtatPerime() throws Exception {
        JsonNode view = startGame(1, 8);
        ObjectNode stale = move(view, view.get("legalActions").get(0));
        stale.put("moveNumber", view.get("moveNumber").asInt() + 1);

        postJson("/api/game/moves", stale)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void refuseUnCoupIllegal() throws Exception {
        JsonNode view = startGame(1, 9);
        ObjectNode illegal = json.createObjectNode().put("type", "Recruter").put("specialistId", "inconnu");

        postJson("/api/game/moves", move(view, illegal)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void refuseUnCoupMalForme() throws Exception {
        JsonNode view = startGame(1, 10);

        JsonNode legal = view.get("legalActions").get(0);

        postJson("/api/game/moves", move(view, json.createObjectNode().put("type", "Tricher")))
                .andExpect(status().isBadRequest());
        postJson("/api/game/moves", move(view, legal).put("bonus", 1))
                .andExpect(status().isBadRequest());
        postJson("/api/game/moves", json.createObjectNode().set("action", legal))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refuseUneDemandeDePartieHorsBornes() throws Exception {
        postJson("/api/game", new NewGameRequest(3, 2, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Mission inconnue ou pas encore jouable"));
        postJson("/api/game", new NewGameRequest(1, 4, null)).andExpect(status().isBadRequest());
    }

    @Test
    void nAccepteQueDuJsonPourModifierLaPartie() throws Exception {
        mvc.perform(post("/api/game").contentType(MediaType.TEXT_PLAIN).content("{\"mission\":1,\"opponents\":1}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/game").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("mission=1&opponents=1"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
