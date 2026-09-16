package io.github.yoyodes1000.endeavor.app.ocean;

import java.util.List;

/**
 * Reflet brut de {@code ocean-tiles.json}, limité à l'identité, aux gains, aux
 * <strong>pistes Sonar</strong> et aux <strong>sites de plongée</strong> des
 * tuiles. Le reste des champs d'activation (conservation, revues, connexions,
 * règles spéciales) n'est pas déclaré : le chargeur les ignore (Phase 2 à venir).
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
            List<Track> sonarTracks,
            List<DiveSite> diveSites) {
    }

    record ArrivalAction(String type) {
    }

    /** Une piste Sonar : ses cases dans l'ordre de lecture. */
    record Track(List<Spot> spots) {
    }

    /** Une case de piste : {@code type} « reward » (gains) ou « discover » (levels). */
    record Spot(String id, String type, List<String> gains, List<Integer> levels) {
    }

    /** Un site de plongée : son identifiant et le nombre de jetons empilés à la pose. */
    record DiveSite(String id, Integer tokens) {
    }
}
