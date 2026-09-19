package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.JournalSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameCount;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.List;
import org.junit.jupiter.api.Test;

class EndGameCounterTest {

    /**
     * Deux zones de profondeur 1 (mêmes sites, pour tester le regroupement par
     * profondeur) et une de profondeur 2 : une piste Sonar (2 cases), deux sites
     * de conservation, un site de publication et un site de plongée par zone.
     */
    private static OceanTileCatalog richCatalog() {
        return new OceanTileCatalog(List.of(
                new OceanTile("site-a", "Site A", 1, false, List.of(), List.of(), List.of(),
                        List.of(new SonarTrack(List.of(new SonarSpot.Reward(List.of()), new SonarSpot.Reward(List.of())))),
                        List.of(new DiveSite("d1", 2)),
                        List.of(new ConservationSite("c1", 0, List.of()), new ConservationSite("c2", 0, List.of())),
                        List.of(new JournalSite("j1", FieldSymbol.BLUE, List.of()))),
                new OceanTile("site-b", "Site B", 2, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", 1)),
                        List.of(new ConservationSite("c1", 0, List.of())),
                        List.of(new JournalSite("j1", FieldSymbol.BLUE, List.of())))));
    }

    private static OceanBoard richBoard() {
        OceanBoard board = new OceanBoard(2);
        board.placeTile(new Cell(1, 0), "site-a");
        board.placeTile(new Cell(1, 1), "site-a");
        board.placeTile(new Cell(2, 0), "site-b");
        return board;
    }

    private static GameState newGame(OceanBoard board) {
        return GameState.newGame(2, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), board, richCatalog(), Fixtures.diveTokenCatalog(), Fixtures.journalCatalog());
    }

    @Test
    void unconditionnelVautToujoursUn() {
        GameState state = newGame(richBoard());
        assertEquals(1, EndGameCounter.effectif(EndGameCount.UNCONDITIONAL, state, 0, "x"));
    }

    @Test
    void compteLesAutresTuilesSeniorHorsCelleQuiCompte() {
        GameState state = newGame(richBoard());
        Player player = state.player(0);
        player.recruit(new HeldSpecialist(Fixtures.ranked("pilot", 1), SpecialistFace.SENIOR, 0));
        player.recruit(new HeldSpecialist(Fixtures.ranked("ecologist", 2), SpecialistFace.JUNIOR, 0));
        player.recruit(new HeldSpecialist(Fixtures.ranked("navigator", 2), SpecialistFace.SENIOR, 0));

        assertEquals(1, EndGameCounter.effectif(EndGameCount.OTHER_SENIOR_SPECIALISTS, state, 0, "navigator"),
                "le pilote Senior compte, la navigatrice (elle-même) et l'écologiste Junior non");
    }

    @Test
    void compteLesSitesDePlongeeVidesSurTouteLaGrille() {
        GameState state = newGame(richBoard());
        OceanBoard board = state.oceanBoard();
        board.stackDiveTokens(new Cell(1, 0), "d1", List.of("research", "research"));
        board.stackDiveTokens(new Cell(1, 1), "d1", List.of("research", "research"));
        board.stackDiveTokens(new Cell(2, 0), "d1", List.of("research"));

        assertEquals(0, EndGameCounter.effectif(EndGameCount.EMPTY_DIVE_SITES, state, 0, "x"), "aucun site vide encore");

        board.takeDiveToken(new Cell(2, 0), "d1");

        assertEquals(1, EndGameCounter.effectif(EndGameCount.EMPTY_DIVE_SITES, state, 0, "x"),
                "un site de plongée global, sans rapport avec le joueur demandé");
    }

    @Test
    void retientLeMaximumDeDisquesDeConservationSurUneSeuleZone() {
        GameState state = newGame(richBoard());
        OceanBoard board = state.oceanBoard();
        board.placeConservationDisc(new Cell(1, 0), "c1", 0);
        board.placeConservationDisc(new Cell(1, 0), "c2", 0);
        board.placeConservationDisc(new Cell(2, 0), "c1", 0);

        assertEquals(2, EndGameCounter.effectif(EndGameCount.CONSERVATION_DISCS_IN_BEST_ZONE, state, 0, "x"));
        assertEquals(0, EndGameCounter.effectif(EndGameCount.CONSERVATION_DISCS_IN_BEST_ZONE, state, 1, "x"),
                "l'autre joueur n'a posé aucun disque");
    }

    @Test
    void compteLesZonesDistinctesAvecAuMoinsUnDisqueDeConservation() {
        GameState state = newGame(richBoard());
        OceanBoard board = state.oceanBoard();
        board.placeConservationDisc(new Cell(1, 0), "c1", 0);
        board.placeConservationDisc(new Cell(1, 0), "c2", 0);
        board.placeConservationDisc(new Cell(2, 0), "c1", 0);

        assertEquals(2, EndGameCounter.effectif(EndGameCount.ZONES_WITH_CONSERVATION, state, 0, "x"),
                "les deux disques de la zone (1,0) ne comptent qu'une fois");
    }

    @Test
    void compteLesProfondeursDistinctesAvecAuMoinsUnePublication() {
        GameState state = newGame(richBoard());
        OceanBoard board = state.oceanBoard();
        board.placeJournalDisc(new Cell(1, 0), "j1", 0);
        board.placeJournalDisc(new Cell(1, 1), "j1", 0);

        assertEquals(1, EndGameCounter.effectif(EndGameCount.DEPTHS_WITH_PUBLICATION, state, 0, "x"),
                "deux zones publiées mais une seule profondeur");

        board.placeJournalDisc(new Cell(2, 0), "j1", 0);

        assertEquals(2, EndGameCounter.effectif(EndGameCount.DEPTHS_WITH_PUBLICATION, state, 0, "x"));
    }

    @Test
    void compteLesZonesAvecUnDisqueQuelconqueOuUnSubmersible() {
        GameState state = newGame(richBoard());
        OceanBoard board = state.oceanBoard();
        board.placeSonarDisc(new Cell(1, 0), 0, 0);
        board.placeConservationDisc(new Cell(1, 1), "c1", 0);
        board.addVessels(new Cell(2, 0), 0, 1);

        assertEquals(3, EndGameCounter.effectif(EndGameCount.ZONES_WITH_DISC_OR_VESSEL, state, 0, "x"));
        assertEquals(0, EndGameCounter.effectif(EndGameCount.ZONES_WITH_DISC_OR_VESSEL, state, 1, "x"));
    }

    @Test
    void lesConnexionsNeSontPasEncoreCablees() {
        GameState state = newGame(richBoard());
        assertThrows(UnsupportedOperationException.class,
                () -> EndGameCounter.effectif(EndGameCount.CONNECTIONS_SHARED_WITH_OPPONENTS, state, 0, "x"));
    }
}
