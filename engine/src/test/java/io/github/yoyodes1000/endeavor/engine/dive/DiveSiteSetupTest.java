package io.github.yoyodes1000.endeavor.engine.dive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiveSiteSetupTest {

    @Test
    void empileUnJetonParSiteDeLaTuile() {
        OceanBoard board = new OceanBoard(1);
        board.placeTile(new Cell(1, 0), "spot");
        OceanTile tile = new OceanTile("spot", "Spot", 1, false, List.of(), List.of(), List.of(), List.of(),
                List.of(new DiveSite("d1", 2)), List.of());
        DiveTokenPile pile = DiveTokenPile.forGame(new DiveTokenCatalog(List.of(
                new DiveToken("research", 5, List.of(new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()))))));

        DiveSiteSetup.stack(board, pile, RandomSource.fromSeed(1), new Cell(1, 0), tile);

        assertEquals(2, board.diveTokenCount(new Cell(1, 0), "d1"));
        assertEquals(3, pile.size(), "2 jetons tirés sur les 5");
    }

    @Test
    void neFaitRienSansSiteDePlongee() {
        OceanBoard board = new OceanBoard(1);
        board.placeTile(new Cell(1, 0), "plain");
        OceanTile tile = new OceanTile("plain", "Plain", 1, false, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        DiveTokenPile pile = DiveTokenPile.forGame(new DiveTokenCatalog(List.of(
                new DiveToken("research", 5, List.of(new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()))))));

        DiveSiteSetup.stack(board, pile, RandomSource.fromSeed(1), new Cell(1, 0), tile);

        assertEquals(5, pile.size(), "rien tiré");
    }
}
