package io.github.yoyodes1000.endeavor.engine.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.dive.DiveOption;
import io.github.yoyodes1000.endeavor.engine.dive.DiveToken;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSetupTest {

    private static GameState game(int players) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());
    }

    @Test
    void chaqueJoueurPoseUnSubmersibleSurLaBaseEtEncaisseSonArrivee() {
        GameState state = game(2);
        int inspirationBefore = state.player(0).attributes().step(Attribute.INSPIRATION);

        GameSetup.deployStartingVessels(state, new Cell(1, 0), 3); // base = atoll (arrivée : inspiration)

        for (int player = 0; player < 2; player++) {
            assertEquals(1, state.oceanBoard().vesselCount(new Cell(1, 0), player), "un submersible sur la base");
            assertEquals(2, state.player(player).vesselStock(), "3 reçus, 1 déployé");
        }
        assertEquals(inspirationBefore + 1, state.player(0).attributes().step(Attribute.INSPIRATION),
                "le bonus d'arrivée de la base est encaissé");
    }

    @Test
    void refuseUneBaseSansTuile() {
        GameState state = game(1);
        assertThrows(IllegalArgumentException.class,
                () -> GameSetup.deployStartingVessels(state, new Cell(3, 2), 3));
    }

    @Test
    void refuseMoinsDUnSubmersibleDeDepart() {
        GameState state = game(1);
        assertThrows(IllegalArgumentException.class,
                () -> GameSetup.deployStartingVessels(state, new Cell(1, 0), 0));
    }

    @Test
    void refuseUnBonusDArriveeDeBaseProduisantUnImpact() {
        OceanBoard ocean = new OceanBoard(1);
        ocean.placeTile(new Cell(1, 0), "base");
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("base", "Base", 1, false, List.of(), List.of(Gain.IMPACT), List.of(), List.of(),
                        List.of(), List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, Fixtures.diveTokenCatalog(), Fixtures.journalCatalog());

        assertThrows(IllegalStateException.class,
                () -> GameSetup.deployStartingVessels(state, new Cell(1, 0), 1));
    }

    @Test
    void empileLesJetonsDesSitesDeplongeeDejaEnJeu() {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "diving-spot");
        ocean.placeTile(new Cell(1, 1), "plain");
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("diving-spot", "Diving Spot", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", 3)), List.of(), List.of()),
                new OceanTile("plain", "Plain", 1, false, List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of())));
        DiveTokenCatalog diveTokenCatalog = new DiveTokenCatalog(List.of(
                new DiveToken("research", 5,
                        List.of(new DiveOption.Gains(List.of(Gain.RESEARCH), List.of())))));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, diveTokenCatalog, Fixtures.journalCatalog());

        GameSetup.stackInitialDiveSites(state);

        assertEquals(3, state.oceanBoard().diveTokenCount(new Cell(1, 0), "d1"), "les 3 jetons du site sont empilés");
        assertEquals(2, state.diveTokenPile().size(), "5 jetons au départ, 3 tirés");
    }
}
