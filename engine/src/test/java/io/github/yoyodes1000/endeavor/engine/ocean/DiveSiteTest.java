package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DiveSiteTest {

    @Test
    void accepteUnSiteValide() {
        assertDoesNotThrow(() -> new DiveSite("d1", 4));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> new DiveSite(" ", 4));
    }

    @Test
    void refuseMoinsDUnJeton() {
        assertThrows(IllegalArgumentException.class, () -> new DiveSite("d1", 0));
    }
}
