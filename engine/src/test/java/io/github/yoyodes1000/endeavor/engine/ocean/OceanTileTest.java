package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class OceanTileTest {

    private static OceanTile tile(String id, int depth) {
        return new OceanTile(id, "Nom", depth, false,
                List.of(Gain.INSPIRATION), List.of(Gain.COORDINATION), List.of());
    }

    @Test
    void accepteUneTuileValide() {
        assertDoesNotThrow(() -> tile("atoll", 1));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> tile("  ", 1));
    }

    @Test
    void refuseUneProfondeurHorsBornes() {
        assertThrows(IllegalArgumentException.class, () -> tile("x", 0));
        assertThrows(IllegalArgumentException.class, () -> tile("x", 6));
    }

    @Test
    void lesGainsSontImmuables() {
        OceanTile tuile = tile("x", 1);
        assertThrows(UnsupportedOperationException.class, () -> tuile.arrivalBonus().add(Gain.IMPACT));
    }
}
