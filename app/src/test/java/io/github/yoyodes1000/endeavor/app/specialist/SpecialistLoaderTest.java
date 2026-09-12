package io.github.yoyodes1000.endeavor.app.specialist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecialistLoaderTest {

    /** Source unique de vérité : le fichier du matériel, à la racine du dépôt. */
    private static final Path CATALOG = Path.of("..", "data", "specialists.json");

    private final SpecialistLoader loader = new SpecialistLoader();

    @Test
    void chargeLeCasierReel() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());

        SpecialistRoster roster;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            roster = loader.load(reader);
        }

        assertEquals(22, roster.specialists().size());
        assertEquals("Team Leader", roster.teamLeader().junior().name());
        assertEquals(9, roster.ofRank(5).size());
        assertTrue(roster.ofRank(5).stream()
                .allMatch(specialist -> specialist.senior().endGameScoring().isPresent()));

        // encodage des actions : le chef d'équipe offre un seul emplacement à cinq choix
        Specialist chef = roster.teamLeader();
        assertEquals(1, chef.junior().actions().size());
        assertEquals(5, chef.junior().actions().get(0).choices().size());

        // un gain précis, pour vérifier la traduction des libellés
        Specialist pilote = roster.byId("pilot").orElseThrow();
        assertEquals(List.of(Gain.IMPACT), pilote.junior().immediateGains());
    }

    @Test
    void refuseUnGainInconnu() {
        String json = document(teamLeader(),
                ranked("pilot", 1, side("Pilot", "[\"gold\"]", "[]"), side("Skipper", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUneActionInconnue() {
        String json = document(teamLeader(),
                ranked("pilot", 1, side("Pilot", "[]", "[[\"teleport\"]]"), side("Skipper", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnDecompteInconnu() {
        String json = document(teamLeader(),
                ranked("mentor", 5, side("Mentor", "[]", "[]"), sideWithEndGame("Renowned Mentor", "compter-tout")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnRangHorsBornes() {
        String json = document(teamLeader(),
                ranked("x", 6, side("X", "[]", "[]"), side("Y", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        String json = document(teamLeader(),
                ranked("dup", 1, side("A", "[]", "[]"), side("B", "[]", "[]")),
                ranked("dup", 2, side("C", "[]", "[]"), side("D", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnRangCinqSansDecompte() {
        String json = document(teamLeader(),
                ranked("mentor", 5, side("Mentor", "[]", "[]"), side("Renowned Mentor", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnCasierSansChefDEquipe() {
        String json = document(
                ranked("pilot", 1, side("Pilot", "[]", "[]"), side("Skipper", "[]", "[]")));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    // --- fabriques de JSON minimal pour les cas invalides ---

    private static String document(String... specialists) {
        return "{\"specialists\":[" + String.join(",", specialists) + "]}";
    }

    private static String teamLeader() {
        return "{\"id\":\"team-leader\",\"teamLeader\":true,\"rank\":null,"
                + "\"junior\":" + side("Team Leader", "[]", "[]") + ","
                + "\"senior\":" + side("Team Leader", "[]", "[]") + "}";
    }

    private static String ranked(String id, int rank, String junior, String senior) {
        return "{\"id\":\"" + id + "\",\"rank\":" + rank
                + ",\"junior\":" + junior + ",\"senior\":" + senior + "}";
    }

    private static String side(String name, String gains, String actions) {
        return "{\"name\":\"" + name + "\",\"immediateGains\":" + gains + ",\"actions\":" + actions + "}";
    }

    private static String sideWithEndGame(String name, String count) {
        return "{\"name\":\"" + name + "\",\"immediateGains\":[],\"actions\":[],"
                + "\"endGameScoring\":{\"text\":\"t\",\"points\":1,\"per\":1,\"count\":\"" + count + "\"}}";
    }
}
