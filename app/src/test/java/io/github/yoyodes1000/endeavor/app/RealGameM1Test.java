package io.github.yoyodes1000.endeavor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.yoyodes1000.endeavor.app.dive.DiveTokenLoader;
import io.github.yoyodes1000.endeavor.app.journal.JournalLoader;
import io.github.yoyodes1000.endeavor.app.mission.MissionLoader;
import io.github.yoyodes1000.endeavor.app.ocean.OceanTileLoader;
import io.github.yoyodes1000.endeavor.app.specialist.SpecialistLoader;
import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.journal.JournalCatalog;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.play.Game;
import io.github.yoyodes1000.endeavor.engine.play.GameSetup;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Assemble une <strong>vraie partie de la mission 1</strong> à partir des fichiers du
 * matériel : charge missions / tuiles / spécialistes, bâtit l'océan via
 * {@code fromSetup}, pose les submersibles de départ sur la base d'opérations, puis
 * démarre la partie. Jalon de vérité : la mise en place réelle tient debout de bout
 * en bout, sans exception, jusqu'au premier point de décision.
 */
class RealGameM1Test {

    private static final Path DATA = Path.of("..", "data");

    private static Reader reader(String file) throws IOException {
        return Files.newBufferedReader(DATA.resolve(file), StandardCharsets.UTF_8);
    }

    @Test
    void assembleEtDemarreUneVraiePartieDeMission1() throws Exception {
        Mission mission1;
        try (Reader source = reader("missions.json")) {
            mission1 = new MissionLoader().load(source).byNumber(1).orElseThrow();
        }
        OceanTileCatalog oceanCatalog;
        try (Reader source = reader("ocean-tiles.json")) {
            oceanCatalog = new OceanTileLoader().load(source);
        }
        SpecialistRoster roster;
        try (Reader source = reader("specialists.json")) {
            roster = new SpecialistLoader().load(source);
        }
        DiveTokenCatalog diveTokenCatalog;
        try (Reader source = reader("dive-tokens.json")) {
            diveTokenCatalog = new DiveTokenLoader().load(source);
        }
        JournalCatalog journalCatalog;
        try (Reader source = reader("journals.json")) {
            journalCatalog = new JournalLoader().load(source);
        }

        RandomSource random = RandomSource.fromSeed(2026);
        OceanBoard ocean = OceanBoard.fromSetup(mission1.oceanSetup(), oceanCatalog, random);

        int players = 3;
        GameState state = GameState.newGame(players, roster, random, 6,
                new MissionBoard(mission1.impactBoard()), ocean, oceanCatalog, diveTokenCatalog, journalCatalog);
        GameSetup.deployStartingVessels(state, mission1.baseOfOperations().orElseThrow(), mission1.startingVessels());
        GameSetup.stackInitialDiveSites(state);
        Game.begin(state);

        // la base d'opérations de M1 : the-sea-star en C1 (colonne C → indice 2)
        Cell base = mission1.baseOfOperations().orElseThrow();
        assertEquals(new Cell(1, 2), base);
        assertEquals("the-sea-star", ocean.tileAt(base).orElseThrow());
        for (int player = 0; player < players; player++) {
            assertEquals(1, ocean.vesselCount(base, player), "un submersible de chaque joueur sur la base");
            assertEquals(0, state.player(player).vesselStock(), "départ à 1, déployé 1");
        }

        // les deux sites de plongée de départ (volcanic-island B1, the-sea-star C1) sont empilés
        assertEquals(2, state.oceanBoard().diveTokenCount(new Cell(1, 1), "d1"), "volcanic-island : 2 jetons");
        assertEquals(4, state.oceanBoard().diveTokenCount(base, "d1"), "the-sea-star : 4 jetons");

        // la partie est démarrée et attend une décision (le premier recrutement)
        assertEquals(GamePhase.PREPARATION, state.phase());
        assertFalse(Game.legalActions(state).isEmpty(), "des coups sont proposés au premier joueur");
    }
}
