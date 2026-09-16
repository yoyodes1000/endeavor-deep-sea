package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConservationSiteTest {

    @Test
    void accepteUnSiteValide() {
        assertDoesNotThrow(() -> new ConservationSite("c1", 2, List.of(Gain.REPUTATION)));
    }

    @Test
    void accepteUnCoutNul() {
        assertDoesNotThrow(() -> new ConservationSite("c1", 0, List.of(Gain.REPUTATION)));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> new ConservationSite(" ", 1, List.of()));
    }

    @Test
    void refuseUnCoutNegatif() {
        assertThrows(IllegalArgumentException.class, () -> new ConservationSite("c1", -1, List.of()));
    }

    @Test
    void lesGainsSontImmuables() {
        ConservationSite site = new ConservationSite("c1", 1, List.of(Gain.IMPACT));
        assertThrows(UnsupportedOperationException.class, () -> site.gains().add(Gain.DISC));
    }
}
