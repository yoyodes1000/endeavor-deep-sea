package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class OceanBoardSetupTest {

    private static OceanTile tile(String id, int depth, boolean unique) {
        return new OceanTile(id, id, depth, unique, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** Trois tuiles de surface hors pioche des uniques, une unique, une de profondeur 2. */
    private static OceanTileCatalog catalog() {
        return new OceanTileCatalog(List.of(
                tile("island", 1, false),
                tile("reef", 1, false),
                tile("kelp", 1, false),
                tile("sea-star", 1, true),
                tile("trench", 2, false)));
    }

    @Test
    void laMiseEnPlacePoseLesTuilesNommees() {
        OceanSetup setup = new OceanSetup(3, List.of(
                new StartingTile.Named(1, 0, "island"),
                new StartingTile.Named(2, 1, "trench")));

        OceanBoard board = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(1));

        assertEquals(3, board.columns());
        assertEquals("island", board.tileAt(new Cell(1, 0)).orElseThrow());
        assertEquals("trench", board.tileAt(new Cell(2, 1)).orElseThrow());
    }

    @Test
    void laMiseEnPlaceRefuseUneTuileNommeeInconnue() {
        OceanSetup setup = new OceanSetup(2, List.of(new StartingTile.Named(1, 0, "inconnue")));
        assertThrows(IllegalArgumentException.class,
                () -> OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(1)));
    }

    @Test
    void leTirageAleatoirePoseUneTuileNonUniqueDuNiveau() {
        OceanSetup setup = new OceanSetup(2, List.of(new StartingTile.Random(1, 0, 1)));

        OceanBoard board = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(3));

        OceanTile placed = catalog().byId(board.tileAt(new Cell(1, 0)).orElseThrow()).orElseThrow();
        assertEquals(1, placed.depth());
        assertFalse(placed.unique(), "une tuile unique n'entre pas dans la pioche");
    }

    @Test
    void leTirageExclutLaTuileNommeeDejaPosee() {
        OceanSetup setup = new OceanSetup(2, List.of(
                new StartingTile.Named(1, 0, "island"),
                new StartingTile.Random(1, 1, 1)));

        OceanBoard board = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(9));

        assertNotEquals("island", board.tileAt(new Cell(1, 1)).orElseThrow());
    }

    @Test
    void deuxTiragesDonnentDesTuilesDifferentes() {
        OceanSetup setup = new OceanSetup(2, List.of(
                new StartingTile.Random(1, 0, 1),
                new StartingTile.Random(1, 1, 1)));

        OceanBoard board = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(5));

        assertNotEquals(board.tileAt(new Cell(1, 0)).orElseThrow(),
                board.tileAt(new Cell(1, 1)).orElseThrow());
    }

    @Test
    void leTirageEchoueQuandLaPileEstVide() {
        OceanSetup setup = new OceanSetup(2, List.of(new StartingTile.Random(5, 0, 5)));
        assertThrows(IllegalArgumentException.class,
                () -> OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(1)));
    }

    @Test
    void laMiseEnPlaceEstDeterministeAGraineEgale() {
        OceanSetup setup = new OceanSetup(2, List.of(new StartingTile.Random(1, 0, 1)));

        OceanBoard first = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(42));
        OceanBoard second = OceanBoard.fromSetup(setup, catalog(), RandomSource.fromSeed(42));

        assertEquals(first.tileAt(new Cell(1, 0)), second.tileAt(new Cell(1, 0)));
    }

    @Test
    void laMiseEnPlaceRefuseZeroColonne() {
        assertThrows(IllegalArgumentException.class, () -> new OceanSetup(0, List.of()));
    }

    @Test
    void laTuileNommeeRefuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> new StartingTile.Named(1, 0, " "));
    }
}
