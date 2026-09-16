package io.github.yoyodes1000.endeavor.app.dive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.dive.DiveOption;
import io.github.yoyodes1000.endeavor.engine.dive.DiveToken;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiveTokenLoaderTest {

    private static final Path CATALOG = Path.of("..", "data", "dive-tokens.json");

    private final DiveTokenLoader loader = new DiveTokenLoader();

    @Test
    void chargeLesDouzeTypesDeJetonsReels() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());

        DiveTokenCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        assertEquals(12, catalog.tokens().size());
        assertEquals(36, catalog.tokens().stream().mapToInt(DiveToken::copies).sum(), "36 jetons au total");

        DiveToken research = catalog.byId("research").orElseThrow();
        assertEquals(6, research.copies());
        assertEquals(1, research.options().size(), "pas de choix : une seule option");
        DiveOption.Gains researchOption = assertInstanceOf(DiveOption.Gains.class, research.options().get(0));
        assertEquals(List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH), researchOption.gains());
        assertTrue(researchOption.cost().isEmpty());
    }

    @Test
    void chargeUneOptionQuiAccordeUneAction() throws Exception {
        DiveTokenCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        DiveToken sonar = catalog.byId("sonar").orElseThrow();
        assertEquals(2, sonar.options().size(), "3 recherche OU un Sonar");
        assertInstanceOf(DiveOption.Gains.class, sonar.options().get(0));
        DiveOption.TriggersAction action = assertInstanceOf(DiveOption.TriggersAction.class, sonar.options().get(1));
        assertEquals(ActionType.SONAR, action.type());
        assertTrue(action.costModifier().isEmpty());
    }

    @Test
    void chargeUnModificateurDeCout() throws Exception {
        DiveTokenCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        DiveToken conserve = catalog.byId("conserve").orElseThrow();
        DiveOption.TriggersAction action =
                assertInstanceOf(DiveOption.TriggersAction.class, conserve.options().get(1));
        assertEquals(ActionType.CONSERVE, action.type());
        assertEquals(-1, action.costModifier().orElseThrow());
    }

    @Test
    void chargeUneOptionAvecCout() throws Exception {
        DiveTokenCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        DiveToken cancelDisc = catalog.byId("cancel-disc").orElseThrow();
        DiveOption.Gains withCost = assertInstanceOf(DiveOption.Gains.class, cancelDisc.options().get(1));
        assertEquals(List.of(Gain.DISC), withCost.cost());
        assertEquals(5, withCost.gains().size());
    }

    @Test
    void refuseUneOptionQuiMelangeGainsEtAction() {
        String json = document(token("t", 1,
                "[{\"gains\":[\"research\"],\"action\":{\"type\":\"sonar\"}}]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUneActionInconnue() {
        String json = document(token("t", 1, "[{\"action\":{\"type\":\"teleport\"}}]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnGainInconnu() {
        String json = document(token("t", 1, "[{\"gains\":[\"gold\"]}]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnJetonSansExemplaires() {
        String json = document(tokenWithoutCopies("t", "[{\"gains\":[\"research\"]}]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnJetonSansOption() {
        String json = document(token("t", 1, "[]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        String json = document(
                token("dup", 1, "[{\"gains\":[\"research\"]}]"),
                token("dup", 2, "[{\"gains\":[\"research\"]}]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    private static String document(String... tokens) {
        return "{\"diveTokens\":[" + String.join(",", tokens) + "]}";
    }

    private static String token(String id, int copies, String options) {
        return "{\"id\":\"" + id + "\",\"copies\":" + copies + ",\"options\":" + options + "}";
    }

    private static String tokenWithoutCopies(String id, String options) {
        return "{\"id\":\"" + id + "\",\"options\":" + options + "}";
    }
}
