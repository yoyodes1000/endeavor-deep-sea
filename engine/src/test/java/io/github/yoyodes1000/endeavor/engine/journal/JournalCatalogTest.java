package io.github.yoyodes1000.endeavor.engine.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JournalCatalogTest {

    private static Journal journal(String id) {
        return new Journal(id, false, id, 1, 0, List.of(FieldSymbol.BLUE), List.of(), List.of());
    }

    @Test
    void refuseUnCatalogueVide() {
        assertThrows(IllegalArgumentException.class, () -> new JournalCatalog(List.of()));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        assertThrows(IllegalArgumentException.class,
                () -> new JournalCatalog(List.of(journal("a"), journal("a"))));
    }

    @Test
    void retrouveParIdentifiant() {
        Journal a = journal("a");
        JournalCatalog catalog = new JournalCatalog(List.of(a, journal("b")));

        assertEquals(a, catalog.byId("a").orElseThrow());
        assertTrue(catalog.byId("z").isEmpty());
    }
}
