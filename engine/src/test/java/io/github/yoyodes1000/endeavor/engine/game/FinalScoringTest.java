package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameCount;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameScoring;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class FinalScoringTest {

    private static final MissionGoal.Standard ONE_POINT_PER_SONAR_DISC = new MissionGoal.Standard(
            1, List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(), 1, Optional.empty(), "sonar");

    private static OceanTileCatalog catalog() {
        return new OceanTileCatalog(List.of(
                new OceanTile("site-a", "Site A", 1, false, List.of(), List.of(), List.of(),
                        List.of(new SonarTrack(List.of(new SonarSpot.Reward(List.of()), new SonarSpot.Reward(List.of())))),
                        List.of(), List.of(), List.of())));
    }

    private static GameState newGame(int players, List<MissionGoal> goals) {
        OceanBoard board = new OceanBoard(2);
        board.placeTile(new Cell(1, 0), "site-a");
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                new MissionBoard(Fixtures.missionBoard().board(), goals), board, catalog(),
                Fixtures.diveTokenCatalog(), Fixtures.journalCatalog());
    }

    private static HeldSpecialist seniorScoring(String id, EndGameCount count) {
        SpecialistSide side = new SpecialistSide(id + "-s", List.of(), List.of(), Optional.empty(),
                Optional.of(new EndGameScoring("texte", 3, 1, count)));
        Specialist specialist = new Specialist(id, OptionalInt.of(5), false, Fixtures.side(id + "-j"), side);
        return new HeldSpecialist(specialist, SpecialistFace.SENIOR, 0);
    }

    @Test
    void additionneLesTroisSourcesEtDesigneLeVainqueur() {
        GameState state = newGame(2, List.of(ONE_POINT_PER_SONAR_DISC));
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);
        state.player(1).recruit(seniorScoring("boss", EndGameCount.UNCONDITIONAL));

        FinalResult result = FinalScoring.compute(state);

        assertEquals(2, result.scores().get(0).missionGoals());
        assertEquals(0, result.scores().get(0).seniorEndGame());
        assertEquals(3, result.scores().get(1).seniorEndGame());
        assertEquals(List.of(1), result.winners(), "3 points de Senior battent 2 points d'objectif");
        assertTrue(result.isComplete());
    }

    @Test
    void uneEgaliteDesigneTousLesJoueursEnTete() {
        FinalResult result = FinalScoring.compute(newGame(3, List.of()));
        assertEquals(List.of(0, 1, 2), result.winners());
    }

    @Test
    void unObjectifNonModeliseComptePourZeroEtMarqueLeScoreIncomplet() {
        GameState state = newGame(2, List.of(new MissionGoal.Unsupported(3, "bonus par couleur")));

        FinalResult result = FinalScoring.compute(state);

        assertEquals(0, result.scores().get(0).missionGoals());
        assertFalse(result.isComplete());
        assertEquals(1, result.uncounted().size(), "un seul signalement, pas un par joueur");
        assertTrue(result.uncounted().get(0).contains("3"));
    }

    @Test
    void unEffectifSeniorNonCableComptePourZeroEtEstSignale() {
        GameState state = newGame(2, List.of());
        state.player(0).recruit(seniorScoring("collector", EndGameCount.COMPLETE_FIELD_SYMBOL_SETS));

        FinalResult result = FinalScoring.compute(state);

        assertEquals(0, result.scores().get(0).seniorEndGame());
        assertFalse(result.isComplete());
        assertTrue(result.uncounted().get(0).contains("collector"));
    }

    @Test
    void uneUniteDObjectifNonCableEstSignalee() {
        MissionGoal.Standard fieldSymbols = new MissionGoal.Standard(
                2, List.of(GoalUnit.FIELD_SYMBOL), List.of(), List.of(), List.of(), 1, Optional.empty(), "symboles");
        // un disque Sonar sur la carte suffit à déclencher le comptage des unités
        GameState state = newGame(2, List.of(fieldSymbols));
        state.oceanBoard().placeSonarDisc(new Cell(1, 0), 0, 0);

        FinalResult result = FinalScoring.compute(state);

        assertFalse(result.isComplete());
        assertTrue(result.uncounted().get(0).contains("Objectif 2"));
    }
}
