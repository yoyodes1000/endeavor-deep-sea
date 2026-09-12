package io.github.yoyodes1000.endeavor.engine.specialist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeMappingTest {

    @Test
    void chaqueGainSeRetrouveParSonCode() {
        for (Gain gain : Gain.values()) {
            assertEquals(gain, Gain.fromCode(gain.code()));
        }
    }

    @Test
    void gainInconnuEchoue() {
        IllegalArgumentException erreur =
                assertThrows(IllegalArgumentException.class, () -> Gain.fromCode("gold"));
        assertTrue(erreur.getMessage().contains("gold"));
    }

    @Test
    void chaqueActionSeRetrouveParSonCode() {
        for (ActionType action : ActionType.values()) {
            assertEquals(action, ActionType.fromCode(action.code()));
        }
    }

    @Test
    void actionInconnueEchoue() {
        assertThrows(IllegalArgumentException.class, () -> ActionType.fromCode("teleport"));
    }

    @Test
    void chaqueDecompteSeRetrouveParSonCode() {
        for (EndGameCount count : EndGameCount.values()) {
            assertEquals(count, EndGameCount.fromCode(count.code()));
        }
    }

    @Test
    void decompteInconnuEchoue() {
        assertThrows(IllegalArgumentException.class, () -> EndGameCount.fromCode("tout-compter"));
    }

    @Test
    void leRegistreCompteLesNeufDecomptes() {
        assertEquals(9, EndGameCount.values().length);
    }
}
