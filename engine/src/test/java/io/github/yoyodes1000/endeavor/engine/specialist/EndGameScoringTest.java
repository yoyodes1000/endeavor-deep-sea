package io.github.yoyodes1000.endeavor.engine.specialist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EndGameScoringTest {

    @Test
    void leScoreEstPointsFoisLaDivisionEntiere() {
        EndGameScoring scoring = new EndGameScoring("x", 1, 3, EndGameCount.ZONES_WITH_DISC_OR_VESSEL);
        assertEquals(0, scoring.score(2));
        assertEquals(1, scoring.score(3));
        assertEquals(1, scoring.score(5));
        assertEquals(2, scoring.score(6));
    }

    @Test
    void leScoreMultiplieLesPoints() {
        EndGameScoring scoring = new EndGameScoring("x", 3, 1, EndGameCount.COMPLETE_FIELD_SYMBOL_SETS);
        assertEquals(9, scoring.score(3));
    }

    @Test
    void refuseDesPointsNegatifs() {
        assertThrows(IllegalArgumentException.class,
                () -> new EndGameScoring("x", -1, 1, EndGameCount.UNCONDITIONAL));
    }

    @Test
    void refuseUnDiviseurInferieurAUn() {
        assertThrows(IllegalArgumentException.class,
                () -> new EndGameScoring("x", 1, 0, EndGameCount.UNCONDITIONAL));
    }

    @Test
    void refuseUnTexteVide() {
        assertThrows(IllegalArgumentException.class,
                () -> new EndGameScoring("  ", 1, 1, EndGameCount.UNCONDITIONAL));
    }
}
