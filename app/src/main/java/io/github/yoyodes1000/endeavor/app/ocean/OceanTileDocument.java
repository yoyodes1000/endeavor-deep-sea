package io.github.yoyodes1000.endeavor.app.ocean;

import java.util.List;

/**
 * Reflet brut de {@code ocean-tiles.json}, limité à l'identité, aux gains et aux
 * <strong>pistes Sonar</strong> des tuiles. Le reste des champs d'activation
 * (autres sites, conservation, revues, connexions, règles spéciales) n'est pas
 * déclaré : le chargeur les ignore (Phase 2 à venir).
 */
record OceanTileDocument(List<Entry> oceanTiles) {

    record Entry(
            String id,
            String name,
            Integer depth,
            Boolean unique,
            List<String> discoverBonus,
            List<String> arrivalBonus,
            List<ArrivalAction> arrivalActions,
            List<Track> sonarTracks) {
    }

    record ArrivalAction(String type) {
    }

    /** Une piste Sonar : ses cases dans l'ordre de lecture. */
    record Track(List<Spot> spots) {
    }

    /** Une case de piste : {@code type} « reward » (gains) ou « discover » (levels). */
    record Spot(String id, String type, List<String> gains, List<Integer> levels) {
    }
}
