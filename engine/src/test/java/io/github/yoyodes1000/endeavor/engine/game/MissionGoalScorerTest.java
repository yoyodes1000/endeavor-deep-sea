package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.MajorityBonus;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.JournalSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MissionGoalScorerTest {

    /** Deux zones de profondeur 1 (colonnes 0 et 1) et une de profondeur 2 (colonne 0). */
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

    private static GameState newGame(int players) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), richBoard(), richCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());
    }

    private static MissionGoal.Standard goal(List<GoalUnit> units, List<Integer> depths, List<Integer> columns,
                                             List<GoalUnit> zoneContains, MajorityBonus bonus) {
        return new MissionGoal.Standard(1, units, depths, columns, zoneContains, 1, Optional.ofNullable(bonus),
                "texte");
    }

    @Test
    void compteLesDisquesDUnTypeSurToutLOcean() {
        GameState state = newGame(2);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);

        MissionGoal.Standard goal = goal(List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(), null);
        assertEquals(2, MissionGoalScorer.effectif(goal, state, 0));
    }

    @Test
    void filtreParColonne() {
        GameState state = newGame(2);
        state.oceanBoard().placeConservationDisc(new Cell(1, 0), "c1", 0);
        state.oceanBoard().placeConservationDisc(new Cell(1, 0), "c2", 0);
        state.oceanBoard().placeConservationDisc(new Cell(1, 1), "c1", 0); // colonne 1, exclue

        MissionGoal.Standard goal = goal(List.of(GoalUnit.CONSERVE), List.of(), List.of(0), List.of(), null);
        assertEquals(2, MissionGoalScorer.effectif(goal, state, 0));
    }

    @Test
    void filtreParProfondeur() {
        GameState state = newGame(2);
        state.oceanBoard().placeJournalDisc(new Cell(1, 0), "j1", 0); // profondeur 1, exclue
        state.oceanBoard().placeJournalDisc(new Cell(2, 0), "j1", 0); // profondeur 2, incluse

        MissionGoal.Standard goal = goal(List.of(GoalUnit.PUBLISH), List.of(2), List.of(), List.of(), null);
        assertEquals(1, MissionGoalScorer.effectif(goal, state, 0));
    }

    @Test
    void compteLesZonesQualifieesParZoneContains() {
        GameState state = newGame(2);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.oceanBoard().placeConservationDisc(new Cell(1, 1), "c1", 0);
        state.oceanBoard().addVessels(new Cell(2, 0), 0, 1); // submersible seul, pas un disque

        MissionGoal.Standard goal = goal(List.of(GoalUnit.ZONE), List.of(), List.of(), List.of(GoalUnit.DISC), null);
        assertEquals(2, MissionGoalScorer.effectif(goal, state, 0));
    }

    @Test
    void unMeneurSeulEncaisseLeBonusEntier() {
        GameState state = newGame(3);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);

        MissionGoal.Standard goal = goal(List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(),
                new MajorityBonus(5, 3));

        assertEquals(2 + 5, MissionGoalScorer.score(goal, state, 0), "2 points d'effectif + le bonus du 1er");
        assertEquals(0, MissionGoalScorer.score(goal, state, 1), "aucun disque, aucun bonus même à égalité à zéro");
    }

    @Test
    void deuxExAequoEnTeteSePartagentLaSommeDesDeuxTranchesArrondieAuInferieur() {
        GameState state = newGame(3);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.oceanBoard().placeSonarDisc(new Cell(1, 1), 0, 1);

        MissionGoal.Standard goal = goal(List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(),
                new MajorityBonus(5, 4));

        // (5+4)/2 = 4 (arrondi à l'inférieur), pour chacun des deux meneurs ex æquo à 1 disque.
        assertEquals(1 + 4, MissionGoalScorer.score(goal, state, 0));
        assertEquals(1 + 4, MissionGoalScorer.score(goal, state, 1));
        assertEquals(0, MissionGoalScorer.score(goal, state, 2), "3e joueur sans disque, aucun bonus");
    }

    @Test
    void lUniteImpactMarkerNEstPasEncoreCablee() {
        GameState state = newGame(2);
        MissionGoal.Standard goal = goal(List.of(GoalUnit.IMPACT_MARKER), List.of(), List.of(), List.of(), null);
        assertThrows(UnsupportedOperationException.class, () -> MissionGoalScorer.effectif(goal, state, 0));
    }
}
