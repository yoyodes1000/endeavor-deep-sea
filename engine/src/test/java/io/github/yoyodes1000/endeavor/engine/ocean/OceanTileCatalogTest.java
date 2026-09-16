package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class OceanTileCatalogTest {

    private static OceanTile tile(String id, int depth) {
        return new OceanTile(id, id, depth, false, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void refuseUnCatalogueVide() {
        assertThrows(IllegalArgumentException.class, () -> new OceanTileCatalog(List.of()));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        assertThrows(IllegalArgumentException.class,
                () -> new OceanTileCatalog(List.of(tile("a", 1), tile("a", 2))));
    }

    @Test
    void retrouveParIdentifiantEtParProfondeur() {
        OceanTile a = tile("a", 1);
        OceanTile b = tile("b", 2);
        OceanTile c = tile("c", 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(a, b, c));

        assertEquals(a, catalog.byId("a").orElseThrow());
        assertTrue(catalog.byId("z").isEmpty());
        assertEquals(List.of(a, c), catalog.ofDepth(1));
        assertEquals(List.of(b), catalog.ofDepth(2));
    }
}
