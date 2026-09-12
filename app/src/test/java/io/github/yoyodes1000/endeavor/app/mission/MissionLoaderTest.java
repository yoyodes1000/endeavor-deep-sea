package io.github.yoyodes1000.endeavor.app.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissionLoaderTest {

    private static final Path CATALOG = Path.of("..", "data", "missions.json");

    private final MissionLoader loader = new MissionLoader();

    private MissionCatalog realCatalog() throws IOException {
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            return loader.load(reader);
        }
    }

    @Test
    void chargeLesDixMissions() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());
        MissionCatalog catalog = realCatalog();

        assertEquals(10, catalog.missions().size());
        Mission mission1 = catalog.byNumber(1).orElseThrow();
        assertEquals(HexOrientation.POINTY_TOP, mission1.impactBoard().orientation());
        assertEquals(32, mission1.impactBoard().hexes().size());
    }

    @Test
    void toutLePlateauDeMission1EstAtteignableDepuisLesDeparts() throws Exception {
        ImpactBoard board = realCatalog().byNumber(1).orElseThrow().impactBoard();

        assertEquals(4, board.startHexes().size());
        assertEquals(board.hexes().size(), board.reachableFromStarts().size(),
                "contrôle de relevé (déc. 7) : tout hexagone atteignable depuis un départ");
    }

    @Test
    void uneCaseDeDepartPorteSaRecompense() throws Exception {
        ImpactHex depart = realCatalog().byNumber(1).orElseThrow().impactBoard().hexAt(0, 0).orElseThrow();
        assertTrue(depart.start());
        assertEquals(List.of(Gain.REPUTATION), depart.gains());
    }

    @Test
    void refuseUnGainInconnu() {
        String json = "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\",\"impactBoard\":"
                + "{\"orientation\":\"pointy-top\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0,\"gains\":[\"gold\"]}]}}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUneOrientationInconnue() {
        String json = "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\",\"impactBoard\":"
                + "{\"orientation\":\"triangle\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0}]}}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }
}
