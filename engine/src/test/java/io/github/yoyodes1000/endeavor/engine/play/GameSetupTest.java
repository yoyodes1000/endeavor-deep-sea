package io.github.yoyodes1000.endeavor.engine.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
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
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog());
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
                new OceanTile("base", "Base", 1, false, List.of(), List.of(Gain.IMPACT), List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog);

        assertThrows(IllegalStateException.class,
                () -> GameSetup.deployStartingVessels(state, new Cell(1, 0), 1));
    }
}
