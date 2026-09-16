package io.github.yoyodes1000.endeavor.engine.dive;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiveTokenTest {

    private static DiveOption.Gains researchOption() {
        return new DiveOption.Gains(List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH), List.of());
    }

    @Test
    void accepteUnJetonValide() {
        assertDoesNotThrow(() -> new DiveToken("research", 6, List.of(researchOption())));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> new DiveToken(" ", 6, List.of(researchOption())));
    }

    @Test
    void refuseMoinsDUnExemplaire() {
        assertThrows(IllegalArgumentException.class, () -> new DiveToken("research", 0, List.of(researchOption())));
    }

    @Test
    void refuseAucuneOption() {
        assertThrows(IllegalArgumentException.class, () -> new DiveToken("research", 6, List.of()));
    }

    @Test
    void unTriggersActionRefuseUnTypeNul() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiveOption.TriggersAction(null, java.util.OptionalInt.empty()));
    }
}
