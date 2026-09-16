package io.github.yoyodes1000.endeavor.engine.dive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiveTokenCatalogTest {

    private static DiveToken token(String id, int copies) {
        return new DiveToken(id, copies,
                List.of(new DiveOption.Gains(List.of(Gain.RESEARCH), List.of())));
    }

    @Test
    void refuseUnCatalogueVide() {
        assertThrows(IllegalArgumentException.class, () -> new DiveTokenCatalog(List.of()));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiveTokenCatalog(List.of(token("a", 1), token("a", 2))));
    }

    @Test
    void retrouveParIdentifiant() {
        DiveToken a = token("a", 1);
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(a, token("b", 2)));

        assertEquals(a, catalog.byId("a").orElseThrow());
        assertTrue(catalog.byId("z").isEmpty());
    }
}
