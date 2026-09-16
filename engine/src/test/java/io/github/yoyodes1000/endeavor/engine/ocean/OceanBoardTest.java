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
    void lesDisquesDeSonarSeComptentParPisteEtSeRemplissentDeGaucheADroite() {
        OceanBoard board = crossBoard();
        assertEquals(0, board.sonarDiscCount(new Cell(1, 1), 0), "piste vierge : la case libre est la première");

        board.placeSonarDisc(new Cell(1, 1), 0, 0);
        board.placeSonarDisc(new Cell(1, 1), 0, 1);

        assertEquals(2, board.sonarDiscCount(new Cell(1, 1), 0), "deux disques posés");
        assertEquals(0, board.sonarDiscCount(new Cell(1, 1), 1), "une autre piste de la même zone reste vierge");
        assertEquals(0, board.sonarDiscCount(new Cell(1, 2), 0), "une autre zone reste vierge");
    }

    @Test
    void onNePeutPasPoserUnDisqueDeSonarSurUneCaseVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class, () -> board.placeSonarDisc(new Cell(2, 0), 0, 0));
    }

    @Test
    void laCopieEstIndependante() {
        OceanBoard original = crossBoard();
        original.addVessels(new Cell(1, 1), 0, 1);
        original.placeSonarDisc(new Cell(1, 1), 0, 0);
        original.stackDiveTokens(new Cell(1, 1), "d1", List.of("research", "sonar"));

        OceanBoard copie = original.copy();
        copie.addVessels(new Cell(1, 1), 0, 3);
        copie.placeTile(new Cell(2, 0), "x");
        copie.placeSonarDisc(new Cell(1, 1), 0, 0);
        copie.takeDiveToken(new Cell(1, 1), "d1");

        assertEquals(1, original.vesselCount(new Cell(1, 1), 0), "les submersibles de l'original ne bougent pas");
        assertFalse(original.isOccupied(new Cell(2, 0)), "la tuile ajoutée à la copie n'existe pas dans l'original");
        assertEquals(1, original.sonarDiscCount(new Cell(1, 1), 0), "les disques de Sonar de l'original ne bougent pas");
        assertEquals(2, original.diveTokenCount(new Cell(1, 1), "d1"), "le site de l'original reste plein");
        assertEquals(4, copie.vesselCount(new Cell(1, 1), 0));
        assertEquals(2, copie.sonarDiscCount(new Cell(1, 1), 0));
        assertEquals(1, copie.diveTokenCount(new Cell(1, 1), "d1"), "un jeton pris sur la copie");
    }

    // --- Sites de plongée ---------------------------------------------------

    @Test
    void lesJetonsDePlongeeSempilentEtSePrennentDuSommet() {
        OceanBoard board = crossBoard();
        assertEquals(0, board.diveTokenCount(new Cell(1, 1), "d1"), "site non encore empilé");

        board.stackDiveTokens(new Cell(1, 1), "d1", List.of("research", "sonar", "cancel-disc"));

        assertEquals(3, board.diveTokenCount(new Cell(1, 1), "d1"));
        assertEquals("research", board.takeDiveToken(new Cell(1, 1), "d1"), "le premier de la liste est le sommet");
        assertEquals(2, board.diveTokenCount(new Cell(1, 1), "d1"));
        assertEquals("sonar", board.takeDiveToken(new Cell(1, 1), "d1"));
    }

    @Test
    void unSiteNePeutEtreEmpileQuUneFois() {
        OceanBoard board = crossBoard();
        board.stackDiveTokens(new Cell(1, 1), "d1", List.of("research"));
        assertThrows(IllegalArgumentException.class,
                () -> board.stackDiveTokens(new Cell(1, 1), "d1", List.of("sonar")));
    }

    @Test
    void onNePeutPasEmpilerSurUneCaseVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class,
                () -> board.stackDiveTokens(new Cell(2, 0), "d1", List.of("research")));
    }

    @Test
    void onNePeutPasPrendreUnJetonDUnSiteVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class, () -> board.takeDiveToken(new Cell(1, 1), "d1"));

        board.stackDiveTokens(new Cell(1, 1), "d1", List.of("research"));
        board.takeDiveToken(new Cell(1, 1), "d1");
        assertThrows(IllegalArgumentException.class, () -> board.takeDiveToken(new Cell(1, 1), "d1"));
    }

    @Test
    void lesCasesOccupeesSontOrdonneesParProfondeurPuisColonne() {
        OceanBoard board = crossBoard();
        assertEquals(List.of(new Cell(1, 0), new Cell(1, 1), new Cell(1, 2), new Cell(2, 1), new Cell(3, 1)),
                board.occupiedCells());
    }

    // --- Sites de conservation -----------------------------------------------

    @Test
    void unSiteDeConservationEstLibreTantQuAucunDisqueNYEstPose() {
        OceanBoard board = crossBoard();
        assertFalse(board.conservationSiteOccupied(new Cell(1, 1), "c1"));

        board.placeConservationDisc(new Cell(1, 1), "c1", 0);

        assertTrue(board.conservationSiteOccupied(new Cell(1, 1), "c1"));
        assertFalse(board.conservationSiteOccupied(new Cell(1, 1), "c2"), "un autre site de la même zone reste libre");
        assertFalse(board.conservationSiteOccupied(new Cell(1, 2), "c1"), "une autre zone reste libre");
    }

    @Test
    void unSiteDeConservationNAccueilleQuUnSeulDisque() {
        OceanBoard board = crossBoard();
        board.placeConservationDisc(new Cell(1, 1), "c1", 0);
        assertThrows(IllegalArgumentException.class,
                () -> board.placeConservationDisc(new Cell(1, 1), "c1", 1));
    }

    @Test
    void onNePeutPasPoserUnDisqueDeConservationSurUneCaseVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class,
                () -> board.placeConservationDisc(new Cell(2, 0), "c1", 0));
    }

    @Test
    void laCopieEstIndependantePourLesSitesDeConservation() {
        OceanBoard original = crossBoard();
        original.placeConservationDisc(new Cell(1, 1), "c1", 0);

        OceanBoard copie = original.copy();
        copie.placeConservationDisc(new Cell(1, 1), "c2", 0);

        assertFalse(original.conservationSiteOccupied(new Cell(1, 1), "c2"),
                "le site posé sur la copie n'existe pas dans l'original");
        assertTrue(copie.conservationSiteOccupied(new Cell(1, 1), "c1"), "l'occupation de l'original est reprise");
    }

    // --- Sites de publication -------------------------------------------------

    @Test
    void unSiteDePublicationEstLibreTantQuAucunDisqueNYEstPose() {
        OceanBoard board = crossBoard();
        assertFalse(board.journalSiteOccupied(new Cell(1, 1), "j1"));

        board.placeJournalDisc(new Cell(1, 1), "j1", 0);

        assertTrue(board.journalSiteOccupied(new Cell(1, 1), "j1"));
        assertFalse(board.journalSiteOccupied(new Cell(1, 1), "j2"), "un autre site de la même zone reste libre");
        assertFalse(board.journalSiteOccupied(new Cell(1, 2), "j1"), "une autre zone reste libre");
    }

    @Test
    void unSiteDePublicationNAccueilleQuUnSeulDisque() {
        OceanBoard board = crossBoard();
        board.placeJournalDisc(new Cell(1, 1), "j1", 0);
        assertThrows(IllegalArgumentException.class,
                () -> board.placeJournalDisc(new Cell(1, 1), "j1", 1));
    }

    @Test
    void onNePeutPasPoserUnDisqueDePublicationSurUneCaseVide() {
        OceanBoard board = crossBoard();
        assertThrows(IllegalArgumentException.class,
                () -> board.placeJournalDisc(new Cell(2, 0), "j1", 0));
    }

    @Test
    void laCopieEstIndependantePourLesSitesDePublication() {
        OceanBoard original = crossBoard();
        original.placeJournalDisc(new Cell(1, 1), "j1", 0);

        OceanBoard copie = original.copy();
        copie.placeJournalDisc(new Cell(1, 1), "j2", 0);

        assertFalse(original.journalSiteOccupied(new Cell(1, 1), "j2"),
                "le site posé sur la copie n'existe pas dans l'original");
        assertTrue(copie.journalSiteOccupied(new Cell(1, 1), "j1"), "l'occupation de l'original est reprise");
    }
}
