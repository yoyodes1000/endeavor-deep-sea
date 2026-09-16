package io.github.yoyodes1000.endeavor.app.ocean;

import java.util.List;

/**
 * Reflet brut de {@code ocean-tiles.json}, limité à l'identité, aux gains, aux
 * <strong>pistes Sonar</strong>, aux <strong>sites de plongée</strong>, aux
 * <strong>sites de conservation</strong> et aux <strong>sites de
 * publication</strong> des tuiles. Le reste des champs d'activation
 * (connexions, règles spéciales, actions accordées par un site de
 * conservation) n'est pas déclaré : le chargeur les ignore (Phase 2 à venir).
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
            List<DiveSite> diveSites,
            List<ConservationSite> conservationSites,
            List<JournalSite> journalSites) {
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

    /**
     * Un site de conservation : son identifiant, son coût en recherche et ses
     * gains. Le champ {@code actions} (bonus accordé par de rares sites) n'est
     * pas déclaré ; Jackson l'ignore.
     */
    record ConservationSite(String id, Integer cost, List<String> gains) {
    }

    /**
     * Un site de publication : son identifiant, son symbole de domaine et ses
     * gains propres (rares). Les champs {@code markedSite} et {@code accessFrom}
     * (connexions) ne sont pas déclarés ; Jackson les ignore.
     */
    record JournalSite(String id, String fieldSymbol, List<String> gains) {
    }
}
