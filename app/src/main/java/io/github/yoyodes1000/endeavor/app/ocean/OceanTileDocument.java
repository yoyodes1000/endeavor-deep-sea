package io.github.yoyodes1000.endeavor.app.ocean;

import java.util.List;

/**
 * Reflet brut de {@code ocean-tiles.json}, limité à l'identité et aux gains des
 * tuiles. Les champs d'activation (sites, sonar, conservation, revues,
 * connexions, règles spéciales) ne sont volontairement pas déclarés : le
 * chargeur les ignore (Phase 2 à venir).
 */
record OceanTileDocument(List<Entry> oceanTiles) {

    record Entry(
            String id,
            String name,
            Integer depth,
            Boolean unique,
            List<String> discoverBonus,
            List<String> arrivalBonus,
            List<ArrivalAction> arrivalActions) {
    }

    record ArrivalAction(String type) {
    }
}
