package io.github.yoyodes1000.endeavor.engine.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.ChoisirOption;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.game.FinalResult;
import io.github.yoyodes1000.endeavor.engine.game.FinalScoring;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.mission.GoalOption;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.mission.SeaStarSide;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Le choix d'option d'un objectif (M8) de bout en bout : le pion posé sur l'hexagone objectif
 * bloque le jeu jusqu'au choix, l'option vaut pour tous, et un objectif dont l'hexagone reste
 * libre ne rapporte rien à personne.
 */
class ChoixOptionObjectifTest {

    private static MissionGoal.Standard zones(SeaStarSide side) {
        return new MissionGoal.Standard(1, List.of(GoalUnit.ZONE), List.of(), List.of(), List.of(GoalUnit.DISC), 1,
                Optional.empty(), "zones", false, List.of(), List.of(), Optional.of(side));
    }

    private static final MissionGoal.Choice NEAR_OR_FAR = new MissionGoal.Choice(1, List.of(
            new GoalOption("near", zones(SeaStarSide.LEFT)), new GoalOption("far", zones(SeaStarSide.RIGHT))));

    private static OceanTileCatalog catalog() {
        SonarTrack track = new SonarTrack(List.of(new SonarSpot.Reward(List.of()), new SonarSpot.Reward(List.of())));
        return new OceanTileCatalog(List.of(
                new OceanTile("the-sea-star", "Sea Star", 1, false, List.of(), List.of(), List.of(), List.of(track),
                        List.of(), List.of(), List.of()),
                new OceanTile("site", "Site", 1, false, List.of(), List.of(), List.of(), List.of(track),
                        List.of(), List.of(), List.of())));
    }

    /** Sea-star en colonne 1 ; zones voisines en colonnes 0 (gauche) et 2 (droite). */
    private static GameState newGame() {
        OceanBoard ocean = new OceanBoard(3);
        ocean.placeTile(new Cell(1, 0), "site");
        ocean.placeTile(new Cell(1, 1), "the-sea-star");
        ocean.placeTile(new Cell(1, 2), "site");
        ocean.placeSonarDisc(new Cell(1, 0), 0, 0); // joueur 0 : à gauche
        ocean.placeSonarDisc(new Cell(1, 2), 0, 1); // joueur 1 : à droite
        ImpactHex goalHex = new ImpactHex(0, 0, 0, List.of(), true, false, false, Optional.empty(), false, 0,
                OptionalInt.of(1));
        ImpactHex plain = new ImpactHex(0, 1, 0, List.of(), true, false, false);
        MissionBoard missions = new MissionBoard(
                new ImpactBoard(HexOrientation.POINTY_TOP, List.of(goalHex, plain)), List.of(NEAR_OR_FAR));
        return GameState.newGame(2, Fixtures.roster(), RandomSource.fromSeed(1), 10, missions, ocean, catalog(),
                Fixtures.diveTokenCatalog(), Fixtures.journalCatalog());
    }

    private static void placeOnGoalHex(GameState state, int player) {
        state.missionBoard().place(state.missionBoard().board().hexAt(0, 0).orElseThrow(), player);
    }

    @Test
    void lePionSurLHexagoneObjectifNeProposeQueLeChoixDeLOption() {
        GameState state = newGame();
        Game.begin(state);
        placeOnGoalHex(state, 1);

        assertEquals(List.<Action>of(new ChoisirOption("near"), new ChoisirOption("far")), Game.legalActions(state));
    }

    @Test
    void toutAutreCoupEstRefuseTantQueLOptionNEstPasChoisie() {
        GameState state = newGame();
        Game.begin(state);
        placeOnGoalHex(state, 0);

        assertThrows(IllegalStateException.class, () -> Game.apply(state, new Passer()));
        Game.apply(state, new ChoisirOption("far"));

        assertFalse(Game.legalActions(state).contains(new ChoisirOption("far")));
        assertEquals("far", state.missionBoard().chosenOption(1).orElseThrow().id());
    }

    @Test
    void lOptionChoisieVautPourTousLesJoueurs() {
        GameState state = newGame();
        Game.begin(state);
        placeOnGoalHex(state, 1);
        Game.apply(state, new ChoisirOption("far"));

        FinalResult result = FinalScoring.compute(state);

        assertEquals(0, result.scores().get(0).missionGoals(), "zone à gauche : hors option far");
        assertEquals(1, result.scores().get(1).missionGoals(), "zone à droite : dans l'option far");
        assertTrue(result.isComplete());
    }

    @Test
    void uneAutreOptionChangeLeCote() {
        GameState state = newGame();
        Game.begin(state);
        placeOnGoalHex(state, 1);
        Game.apply(state, new ChoisirOption("near"));

        FinalResult result = FinalScoring.compute(state);

        assertEquals(1, result.scores().get(0).missionGoals());
        assertEquals(0, result.scores().get(1).missionGoals());
    }

    @Test
    void unHexagoneObjectifLibreNeRapporteRienAPersonne() {
        GameState state = newGame();

        FinalResult result = FinalScoring.compute(state);

        assertEquals(0, result.scores().get(0).missionGoals());
        assertEquals(0, result.scores().get(1).missionGoals());
        assertTrue(result.isComplete(), "un objectif non choisi n'est pas un manque du moteur");
    }
}
