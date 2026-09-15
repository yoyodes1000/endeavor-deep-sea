package io.github.yoyodes1000.endeavor.app.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OceanTileLoaderTest {

    private static final Path CATALOG = Path.of("..", "data", "ocean-tiles.json");

    private final OceanTileLoader loader = new OceanTileLoader();

    @Test
    void chargeLesTrenteSeptTuilesReelles() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());

        OceanTileCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        assertEquals(37, catalog.tiles().size());
        assertEquals(3, catalog.tiles().stream().filter(OceanTile::unique).count());

        OceanTile atoll = catalog.byId("atoll").orElseThrow();
        assertEquals(1, atoll.depth());
        assertEquals(List.of(Gain.INSPIRATION, Gain.DISC), atoll.discoverBonus());
        assertEquals(List.of(Gain.COORDINATION), atoll.arrivalBonus());

        for (int depth = 1; depth <= 5; depth++) {
            assertFalse(catalog.ofDepth(depth).isEmpty(), "profondeur " + depth);
        }
    }

    @Test
    void chargeLesPistesSonarRecompenseEtDecouverte() throws Exception {
        OceanTileCatalog catalog;
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            catalog = loader.load(reader);
        }

        OceanTile calmSeas = catalog.byId("calm-seas").orElseThrow();
        assertEquals(2, calmSeas.sonarTracks().size(), "calm-seas a deux pistes Sonar");

        List<SonarSpot> firstTrack = calmSeas.sonarTracks().get(0).spots();
        SonarSpot reward = firstTrack.get(0);
        assertTrue(reward instanceof SonarSpot.Reward, "la première case est une récompense");
        assertEquals(List.of(Gain.INSPIRATION, Gain.RESEARCH), ((SonarSpot.Reward) reward).gains());
        SonarSpot discover = firstTrack.get(1);
        assertTrue(discover instanceof SonarSpot.Discover, "la seconde case est une découverte");
        assertEquals(List.of(3, 4, 5), ((SonarSpot.Discover) discover).levels());
    }

    @Test
    void refuseUnTypeDeCaseSonarInconnu() {
        String json = "{\"oceanTiles\":[{\"id\":\"t\",\"name\":\"T\",\"depth\":1,\"unique\":false,"
                + "\"discoverBonus\":[],\"arrivalBonus\":[],"
                + "\"sonarTracks\":[{\"spots\":[{\"id\":\"s1\",\"type\":\"echo\"}]}]}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void ignoreLesChampsDActivationNonModelises() {
        String json = "{\"oceanTiles\":[{\"id\":\"t\",\"name\":\"T\",\"depth\":1,\"unique\":false,"
                + "\"discoverBonus\":[\"disc\"],\"arrivalBonus\":[\"coordination\"],"
                + "\"diveSites\":[{\"id\":\"d1\",\"tokens\":4}],\"sonarTracks\":[],\"specialRules\":[]}]}";
        OceanTileCatalog catalog = loader.load(new StringReader(json));

        assertEquals(1, catalog.tiles().size());
        assertEquals(List.of(Gain.DISC), catalog.byId("t").orElseThrow().discoverBonus());
    }

    @Test
    void refuseUnGainInconnu() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new StringReader(document(tile("t", 1, "[\"gold\"]", "[]")))));
    }

    @Test
    void refuseUneProfondeurHorsBornes() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new StringReader(document(tile("t", 9, "[]", "[]")))));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        String json = document(tile("dup", 1, "[]", "[]"), tile("dup", 2, "[]", "[]"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    private static String document(String... tiles) {
        return "{\"oceanTiles\":[" + String.join(",", tiles) + "]}";
    }

    private static String tile(String id, int depth, String discover, String arrival) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"depth\":" + depth
                + ",\"unique\":false,\"discoverBonus\":" + discover + ",\"arrivalBonus\":" + arrival + "}";
    }
}
