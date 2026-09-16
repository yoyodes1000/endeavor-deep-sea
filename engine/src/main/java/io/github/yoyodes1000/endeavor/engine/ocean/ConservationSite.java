package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Un site de conservation d'une tuile : l'action Conservation y paie
 * {@code cost} points de recherche, y pose un disque, puis encaisse
 * {@code gains}. Un site n'accueille qu'un seul disque, jamais repris —
 * l'occupation vit dans {@link OceanBoard}, comme pour les pistes Sonar et
 * les sites de plongée.
 *
 * @param id    l'identifiant du site sur sa tuile (unique par tuile)
 * @param cost  le coût en recherche à payer pour ce site (peut être nul)
 * @param gains les gains encaissés à la pose du disque
 */
public record ConservationSite(String id, int cost, List<Gain> gains) {

    public ConservationSite {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un site de conservation doit avoir un identifiant");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Coût de conservation négatif pour " + id + " : " + cost);
        }
        gains = List.copyOf(gains);
    }
}
