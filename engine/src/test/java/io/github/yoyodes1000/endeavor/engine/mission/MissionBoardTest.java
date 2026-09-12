package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MissionBoardTest {

    private static ImpactHex start(int row, int col) {
        return new ImpactHex(row, col, 0, List.of(), true, false, false);
    }

    private static ImpactHex hex(int row, int col) {
        return new ImpactHex(row, col, 0, List.of(), false, false, false);
    }

    private static ImpactBoard board() {
        return new ImpactBoard(HexOrientation.POINTY_TOP,
                List.of(start(0, 0), hex(0, 1), hex(1, 0), hex(1, 1)));
    }

    @Test
    void auDepartSeulesLesCasesDeDepartSontLegales() {
        MissionBoard missionBoard = new MissionBoard(board());
        List<ImpactHex> legal = missionBoard.legalPlacements();
        assertEquals(1, legal.size());
        assertTrue(legal.get(0).start());
    }

    @Test
    void poserOuvreLesVoisins() {
        MissionBoard missionBoard = new MissionBoard(board());
        ImpactHex depart = missionBoard.board().hexAt(0, 0).orElseThrow();
        missionBoard.place(depart, 0);

        List<ImpactHex> legal = missionBoard.legalPlacements();
        assertFalse(legal.contains(depart), "la case occupée n'est plus légale");
        assertTrue(legal.contains(missionBoard.board().hexAt(0, 1).orElseThrow()));
        assertTrue(legal.contains(missionBoard.board().hexAt(1, 0).orElseThrow()));
    }

    @Test
    void onNePeutPasPoserSurUneCaseOccupee() {
        MissionBoard missionBoard = new MissionBoard(board());
        ImpactHex depart = missionBoard.board().hexAt(0, 0).orElseThrow();
        missionBoard.place(depart, 0);
        assertThrows(IllegalArgumentException.class, () -> missionBoard.place(depart, 1));
    }

    @Test
    void onNePeutPasPoserLoinDeToutOccupe() {
        MissionBoard missionBoard = new MissionBoard(board());
        ImpactHex isole = missionBoard.board().hexAt(1, 1).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> missionBoard.place(isole, 0));
    }

    @Test
    void lOccupationRetientLePropprietaire() {
        MissionBoard missionBoard = new MissionBoard(board());
        ImpactHex depart = missionBoard.board().hexAt(0, 0).orElseThrow();
        missionBoard.place(depart, 2);
        assertTrue(missionBoard.isOccupied(depart));
        assertEquals(2, missionBoard.owner(depart).orElseThrow());
    }

    @Test
    void laCopieEstIndependante() {
        MissionBoard original = new MissionBoard(board());
        MissionBoard copie = original.copy();
        copie.place(copie.board().hexAt(0, 0).orElseThrow(), 0);
        assertFalse(original.isOccupied(original.board().hexAt(0, 0).orElseThrow()));
    }
}
