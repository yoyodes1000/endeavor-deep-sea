package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameCount;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Les symboles de domaine du plateau Impact : jeux complets (Senior) et couleur la plus possédée (objectif). */
class FieldSymbolScoringTest {

    private static final int BLUE_A = 0;
    private static final int BLUE_B = 1;
    private static final int BROWN = 2;
    private static final int GREEN = 3;
    private static final int YELLOW = 4;
    private static final int WILD = 5;
    private static final int DOUBLE_WILD = 6;
    private static final int PLAIN = 7;

    private static ImpactHex hex(int col, Optional<FieldSymbol> color, boolean wild, int count) {
        return new ImpactHex(0, col, 0, List.of(), true, false, false, color, wild, count);
    }

    /** Huit hexagones de départ (tous posables) : bleu, bleu, brun, vert, jaune, joker, double joker, sans symbole. */
    private static GameState newGame() {
        List<ImpactHex> hexes = new ArrayList<>();
        hexes.add(hex(BLUE_A, Optional.of(FieldSymbol.BLUE), false, 1));
        hexes.add(hex(BLUE_B, Optional.of(FieldSymbol.BLUE), false, 1));
        hexes.add(hex(BROWN, Optional.of(FieldSymbol.BROWN), false, 1));
        hexes.add(hex(GREEN, Optional.of(FieldSymbol.GREEN), false, 1));
        hexes.add(hex(YELLOW, Optional.of(FieldSymbol.YELLOW), false, 1));
        hexes.add(hex(WILD, Optional.empty(), true, 1));
        hexes.add(hex(DOUBLE_WILD, Optional.empty(), true, 2));
        hexes.add(hex(PLAIN, Optional.empty(), false, 0));
        MissionBoard board = new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, hexes));
        return GameState.newGame(2, Fixtures.roster(), RandomSource.fromSeed(1), 10, board,
                Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());
    }

    private static void occupy(GameState state, int player, int... cols) {
        for (int col : cols) {
            ImpactHex hex = state.missionBoard().board().hexAt(0, col).orElseThrow();
            state.missionBoard().place(hex, player);
        }
    }

    private static int sets(GameState state, int player) {
        return EndGameCounter.effectif(EndGameCount.COMPLETE_FIELD_SYMBOL_SETS, state, player, "x");
    }

    private static int mostHeld(GameState state, int player) {
        MissionGoal.Standard goal = new MissionGoal.Standard(
                1, List.of(GoalUnit.FIELD_SYMBOL), List.of(), List.of(), List.of(), 1, Optional.empty(), "t");
        return MissionGoalScorer.effectif(goal, state, player);
    }

    @Test
    void unJeuCompletDesQuatreCouleursVautUnEnsemble() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BROWN, GREEN, YELLOW);
        assertEquals(1, sets(state, 0));
    }

    @Test
    void unJeuIncompletNeVautRien() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BLUE_B, BROWN, GREEN);
        assertEquals(0, sets(state, 0));
    }

    @Test
    void unJokerComblePourLaCouleurManquante() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BROWN, GREEN, WILD);
        assertEquals(1, sets(state, 0));
    }

    @Test
    void unSymboleDoubleCompteDeuxFois() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BROWN, DOUBLE_WILD); // 2 jokers : vert et jaune
        assertEquals(1, sets(state, 0));
    }

    @Test
    void lesJokersRestantsServentAuDeuxiemeEnsemble() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BLUE_B, BROWN, GREEN, YELLOW, WILD, DOUBLE_WILD); // + 3 jokers : brun, vert, jaune
        assertEquals(2, sets(state, 0));
    }

    @Test
    void lesPionsAdversesEtLesCasesSansSymboleNeComptentPas() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, PLAIN);
        occupy(state, 1, BROWN, GREEN, YELLOW);
        assertEquals(0, sets(state, 0));
        assertEquals(0, sets(state, 1));
    }

    @Test
    void laCouleurLaPlusPossedeeAjouteLesJokers() {
        GameState state = newGame();
        occupy(state, 0, BLUE_A, BLUE_B, BROWN, WILD);
        assertEquals(3, mostHeld(state, 0), "2 bleus + 1 joker");
    }

    @Test
    void sansAucunSymboleLaCouleurLaPlusPossedeeVautZero() {
        assertEquals(0, mostHeld(newGame(), 0));
    }

    @Test
    void unHexagoneRefuseUnSymboleIncoherent() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImpactHex(0, 0, 0, List.of(), false, false, false, Optional.of(FieldSymbol.BLUE), true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ImpactHex(0, 0, 0, List.of(), false, false, false, Optional.empty(), false, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ImpactHex(0, 0, 0, List.of(), false, false, false, Optional.empty(), true, 3));
    }
}
