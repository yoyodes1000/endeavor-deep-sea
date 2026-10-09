package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ImpactBoardTest {

    private static ImpactHex hex(int row, int col) {
        return new ImpactHex(row, col, 0, List.of(), false, false, false);
    }

    private static ImpactHex start(int row, int col) {
        return new ImpactHex(row, col, 0, List.of(), true, false, false);
    }

    private static ImpactHex offGrid(int row, int col) {
        return new ImpactHex(row, col, 0, List.of(), false, true, false);
    }

    /** Petite grappe pointy-top connexe. */
    private static ImpactBoard cluster() {
        return new ImpactBoard(HexOrientation.POINTY_TOP,
                List.of(start(0, 0), hex(0, 1), hex(1, 0), hex(1, 1), hex(2, 0)));
    }

    @Test
    void lesVoisinsPointyTop() {
        ImpactBoard board = cluster();
        List<ImpactHex> voisins = board.neighbors(board.hexAt(0, 0).orElseThrow());
        assertEquals(2, voisins.size());
        assertTrue(voisins.contains(board.hexAt(0, 1).orElseThrow()));
        assertTrue(voisins.contains(board.hexAt(1, 0).orElseThrow()));
    }

    @Test
    void lAdjacenceEstSymetrique() {
        ImpactBoard board = cluster();
        for (ImpactHex a : board.hexes()) {
            for (ImpactHex voisin : board.neighbors(a)) {
                assertTrue(board.neighbors(voisin).contains(a),
                        a.row() + "," + a.col() + " voisin de " + voisin.row() + "," + voisin.col());
            }
        }
    }

    @Test
    void toutEstAtteignableDepuisLeDepart() {
        ImpactBoard board = cluster();
        assertEquals(board.hexes().size(), board.reachableFromStarts().size());
    }

    @Test
    void lHexagoneOffGridEstHorsAdjacence() {
        ImpactBoard board = new ImpactBoard(HexOrientation.POINTY_TOP,
                List.of(start(0, 0), hex(0, 1), offGrid(1, 0)));
        assertTrue(board.hexAt(1, 0).isEmpty(), "un hexagone offGrid n'est pas sur la grille");
        assertTrue(board.neighbors(board.hexAt(0, 0).orElseThrow()).stream().noneMatch(ImpactHex::offGrid));
    }

    @Test
    void refuseDeuxHexagonesALaMemePosition() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImpactBoard(HexOrientation.POINTY_TOP, List.of(hex(0, 0), hex(0, 0))));
    }

    @Test
    void lesPistesRectanglesSeSuiventDeGaucheADroiteSansSeToucher() {
        List<ImpactHex> hexes = List.of(
                new ImpactHex(0, 0, 0, List.of(), true, false, false),
                new ImpactHex(0, 1, 0, List.of(), false, false, false),
                new ImpactHex(0, 2, 0, List.of(), false, false, false),
                new ImpactHex(1, 0, 0, List.of(), true, false, false),
                new ImpactHex(1, 1, 0, List.of(), false, false, false));
        ImpactBoard board = new ImpactBoard(HexOrientation.RECT_ROWS, hexes);

        assertEquals(List.of(hexes.get(1)), board.neighbors(hexes.get(0)));
        assertEquals(2, board.neighbors(hexes.get(1)).size(), "un voisin de chaque côté");
        assertEquals(List.of(hexes.get(4)), board.neighbors(hexes.get(3)), "la piste 1 ne touche pas la piste 0");
    }
}
