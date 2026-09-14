package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OceanBoardTest {

    /** Rangée de surface (colonnes 0 à 2) + descente en colonne 1 jusqu'à la profondeur 3. */
    private static OceanBoard crossBoard() {
        OceanBoard board = new OceanBoard(3);
        board.placeTile(new Cell(1, 0), "a");
        board.placeTile(new Cell(1, 1), "b");
        board.placeTile(new Cell(1, 2), "c");
        board.placeTile(new Cell(2, 1), "d");
        board.placeTile(new Cell(3, 1), "e");
        return board;
    }

    @Test
    void laTuilePoseeSeRelit() {
        OceanBoard board = new OceanBoard(3);
        board.placeTile(new Cell(1, 1), "volcanic-island");

        assertTrue(board.isOccupied(new Cell(1, 1)));
        assertEquals("volcanic-island", board.tileAt(new Cell(1, 1)).orElseThrow());
        assertFalse(board.isOccupied(new Cell(1, 0)));
    }

    @Test
    void onNePeutPasPoserSurUneCaseOccupee() {
        OceanBoard board = new OceanBoard(3);
        board.placeTile(new Cell(1, 1), "a");
        assertThrows(IllegalArgumentException.class, () -> board.placeTile(new Cell(1, 1), "b"));
    }

    @Test
    void onNePeutPasPoserHorsDesColonnes() {
        OceanBoard board = new OceanBoard(3);
        assertThrows(IllegalArgumentException.class, () -> board.placeTile(new Cell(1, 3), "a"));
    }

    @Test
    void onNePeutPasPoserPlusProfondQueCinq() {
        OceanBoard board = new OceanBoard(3);
        assertThrows(IllegalArgumentException.class, () -> board.placeTile(new Cell(6, 0), "a"));
    }

    @Test
    void lesVoisinsSontLesCasesOccupeesOrthogonales() {
        OceanBoard board = crossBoard();
        // (1,1) : dessus hors grille, dessous (2,1), gauche (1,0), droite (1,2) — tous occupés
        assertEquals(List.of(new Cell(2, 1), new Cell(1, 0), new Cell(1, 2)),
                board.neighbors(new Cell(1, 1)));
        // (1,0) : dessous (2,0) est vide, donc pas un voisin ; seul (1,1) l'est
        assertEquals(List.of(new Cell(1, 1)), board.neighbors(new Cell(1, 0)));
    }

    @Test
    void leVoyageAtteintEnLigneDroiteSelonLaDistance() {
        OceanBoard board = new OceanBoard(4);
        for (int col = 0; col < 4; col++) {
            board.placeTile(new Cell(1, col), "t" + col);
        }
        Cell origin = new Cell(1, 0);
        assertEquals(Set.of(new Cell(1, 1)), board.reachableFrom(origin, 1));
        assertEquals(Set.of(new Cell(1, 1), new Cell(1, 2)), board.reachableFrom(origin, 2));
        assertEquals(Set.of(new Cell(1, 1), new Cell(1, 2), new Cell(1, 3)),
                board.reachableFrom(origin, 3));
    }

    @Test
    void leVoyageNeFranchitPasUnVide() {
        OceanBoard board = new OceanBoard(3);
        board.placeTile(new Cell(1, 0), "a");
        board.placeTile(new Cell(1, 2), "c"); // trou en (1,1)

        assertTrue(board.reachableFrom(new Cell(1, 0), 5).isEmpty());
    }

    @Test
    void leVoyageEstBorneEnProfondeurParLeNiveau() {
        OceanBoard board = crossBoard();
        // au niveau 1, on ne descend pas en profondeur 2 : seules les cases de surface voisines
        assertEquals(Set.of(new Cell(1, 0), new Cell(1, 2)),
                board.reachableFrom(new Cell(1, 1), 1));
    }

    @Test
    void leVoyageAtteintLaProfondeurEgaleAuNiveau() {
        OceanBoard board = crossBoard();
        Set<Cell> reachable = board.reachableFrom(new Cell(1, 1), 3);
        assertTrue(reachable.contains(new Cell(2, 1)));
        assertTrue(reachable.contains(new Cell(3, 1)), "prof. 3 atteinte au niveau 3");
    }

    @Test
    void leVoyageExclutLOrigine() {
        OceanBoard board = crossBoard();
        assertFalse(board.reachableFrom(new Cell(1, 1), 5).contains(new Cell(1, 1)));
    }

    @Test
    void leVoyageEstNulSansNiveau() {
        OceanBoard board = crossBoard();
        assertTrue(board.reachableFrom(new Cell(1, 1), 0).isEmpty());
    }

    @Test
    void lesSubmersiblesSeComptentParZoneEtParJoueur() {
        OceanBoard board = crossBoard();
        board.addVessels(new Cell(1, 1), 0, 2);

        assertEquals(2, board.vesselCount(new Cell(1, 1), 0));
        assertEquals(0, board.vesselCount(new Cell(1, 1), 1));
        assertEquals(0, board.vesselCount(new Cell(2, 1), 0));
    }

    @Test
    void onNePeutPasPoserUnSubmersibleSurUneCaseVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class, () -> board.addVessels(new Cell(2, 0), 0, 1));
    }

    @Test
    void deplacerUnSubmersibleLeRetireDuDepartEtLAjouteAArrivee() {
        OceanBoard board = crossBoard();
        board.addVessels(new Cell(1, 1), 0, 1);

        board.moveVessel(new Cell(1, 1), new Cell(2, 1), 0);

        assertEquals(0, board.vesselCount(new Cell(1, 1), 0));
        assertEquals(1, board.vesselCount(new Cell(2, 1), 0));
    }

    @Test
    void onNePeutPasDeplacerUnSubmersibleAbsent() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class,
                () -> board.moveVessel(new Cell(1, 0), new Cell(1, 1), 0));
    }

    @Test
    void laCopieEstIndependante() {
        OceanBoard original = crossBoard();
        original.addVessels(new Cell(1, 1), 0, 1);

        OceanBoard copie = original.copy();
        copie.addVessels(new Cell(1, 1), 0, 3);
        copie.placeTile(new Cell(2, 0), "x");

        assertEquals(1, original.vesselCount(new Cell(1, 1), 0), "les submersibles de l'original ne bougent pas");
        assertFalse(original.isOccupied(new Cell(2, 0)), "la tuile ajoutée à la copie n'existe pas dans l'original");
        assertEquals(4, copie.vesselCount(new Cell(1, 1), 0));
    }
}
