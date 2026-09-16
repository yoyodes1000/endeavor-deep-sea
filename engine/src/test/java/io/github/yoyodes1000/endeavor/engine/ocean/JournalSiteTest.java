package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalSiteTest {

    @Test
    void accepteUnSiteValide() {
        assertDoesNotThrow(() -> new JournalSite("j1", FieldSymbol.BLUE, List.of()));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> new JournalSite(" ", FieldSymbol.BLUE, List.of()));
    }

    @Test
    void refuseUnSymboleDeDomaineNul() {
        assertThrows(IllegalArgumentException.class, () -> new JournalSite("j1", null, List.of()));
    }

    @Test
    void lesGainsSontImmuables() {
        JournalSite site = new JournalSite("j1", FieldSymbol.GREEN, List.of(Gain.IMPACT));
        assertThrows(UnsupportedOperationException.class, () -> site.gains().add(Gain.DISC));
    }
}
