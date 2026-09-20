package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.ColorBonus;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.LeaderBonus;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
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
        OceanBoard board = new OceanBoard(3);
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

    private static MissionGoal.Standard discovery(List<LeaderBonus> leaderBonuses) {
        return new MissionGoal.Standard(1, List.of(GoalUnit.ZONE), List.of(), List.of(), List.of(), 1,
                Optional.empty(), "découvertes", true, leaderBonuses, List.of());
    }

    @Test
    void compteLesZonesDecouvertesParLeJoueurSeulement() {
        GameState state = newGame(2);
        state.oceanBoard().discoverTile(new Cell(3, 0), "site-a", 0);
        state.oceanBoard().discoverTile(new Cell(3, 1), "site-a", 1);
        // (1,0), (1,1) et (2,0) sont de mise en place : sans découvreur

        MissionGoal.Standard goal = discovery(List.of());
        assertEquals(1, MissionGoalScorer.effectif(goal, state, 0));
        assertEquals(1, MissionGoalScorer.effectif(goal, state, 1));
    }

    @Test
    void leBonusDeLeaderParProfondeurEtParColonneVaAuMeilleurDecouvreur() {
        GameState state = newGame(3);
        OceanBoard ocean = state.oceanBoard();
        ocean.discoverTile(new Cell(3, 0), "site-a", 0); // joueur 0 : profondeur 3, colonne 0
        ocean.discoverTile(new Cell(3, 1), "site-a", 0); // joueur 0 : profondeur 3, colonne 1
        ocean.discoverTile(new Cell(3, 2), "site-a", 1); // joueur 1 : profondeur 3, colonne 2

        MissionGoal.Standard goal = discovery(List.of(
                new LeaderBonus(List.of(3), List.of(), 2),
                new LeaderBonus(List.of(), List.of(2), 5)));

        assertEquals(2 + 2, MissionGoalScorer.score(goal, state, 0), "2 zones + leader de la profondeur 3");
        assertEquals(1 + 5, MissionGoalScorer.score(goal, state, 1), "1 zone + leader de la colonne 2 (seul)");
        assertEquals(0, MissionGoalScorer.score(goal, state, 2), "aucune zone découverte : aucun bonus");
    }

    @Test
    void lesLeadersExAequoTouchentChacunLeBonusDeLeader() {
        GameState state = newGame(2);
        state.oceanBoard().discoverTile(new Cell(3, 0), "site-a", 0);
        state.oceanBoard().discoverTile(new Cell(3, 1), "site-a", 1);

        MissionGoal.Standard goal = discovery(List.of(new LeaderBonus(List.of(3), List.of(), 2)));

        assertEquals(1 + 2, MissionGoalScorer.score(goal, state, 0));
        assertEquals(1 + 2, MissionGoalScorer.score(goal, state, 1));
    }

    private static GameState stateWithSymbols() {
        List<ImpactHex> hexes = new java.util.ArrayList<>();
        for (int col = 0; col < 6; col++) {
            hexes.add(new ImpactHex(0, col, 0, List.of(), true, false, false,
                    col == 5 ? Optional.empty() : Optional.of(FieldSymbol.values()[col % 2]), col == 5,
                    1));
        }
        MissionBoard board = new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, hexes));
        return GameState.newGame(2, Fixtures.roster(), RandomSource.fromSeed(1), 10, board, richBoard(),
                richCatalog(), Fixtures.diveTokenCatalog(), Fixtures.journalCatalog());
    }

    @Test
    void leBonusParCouleurVaAuPlusGrandNombreDeSymbolesDeCetteCouleurJokersCompris() {
        // colonnes 0,2,4 : bleu ; colonnes 1,3 : jaune ; colonne 5 : joker
        GameState state = stateWithSymbols();
        ImpactBoard impact = state.missionBoard().board();
        for (int col : new int[]{0, 2}) {
            state.missionBoard().place(impact.hexAt(0, col).orElseThrow(), 0); // 2 bleus
        }
        state.missionBoard().place(impact.hexAt(0, 1).orElseThrow(), 1);       // 1 jaune
        state.missionBoard().place(impact.hexAt(0, 5).orElseThrow(), 1);       // 1 joker

        MissionGoal.Standard goal = new MissionGoal.Standard(3, List.of(GoalUnit.FIELD_SYMBOL), List.of(), List.of(),
                List.of(), 1, Optional.empty(), "symboles", false, List.of(),
                List.of(new ColorBonus(FieldSymbol.BLUE, 2), new ColorBonus(FieldSymbol.YELLOW, 3)));

        // joueur 0 : bleu 2 (leader bleu), jaune 0 ; joueur 1 : bleu 1 (joker), jaune 2 (joker compris, leader jaune)
        assertEquals(2 + 2, MissionGoalScorer.score(goal, state, 0), "type le plus possédé : 2 ; leader bleu");
        assertEquals(2 + 3, MissionGoalScorer.score(goal, state, 1), "jaune 1 + joker = 2 ; leader jaune");
    }
}
