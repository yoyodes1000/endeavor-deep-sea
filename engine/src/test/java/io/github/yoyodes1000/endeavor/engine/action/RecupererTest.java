package io.github.yoyodes1000.endeavor.engine.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RecupererTest {

    @Test
    void uneRecuperationViseUnIdentifiantNonVide() {
        assertThrows(IllegalArgumentException.class, () -> new Recuperer("  "));
        assertThrows(IllegalArgumentException.class, () -> new Recuperer(null));
    }

    @Test
    void uneRecuperationRetientLIdentifiantVise() {
        assertEquals("pilot", new Recuperer("pilot").specialistId());
    }
}
