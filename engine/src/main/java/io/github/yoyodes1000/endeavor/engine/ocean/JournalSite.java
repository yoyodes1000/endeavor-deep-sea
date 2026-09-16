package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Un site de publication d'une tuile : l'action Publication n'y pose un
 * disque que si la revue visée porte le symbole de domaine {@code fieldSymbol}
 * du site. Un site n'accueille qu'un seul disque, jamais repris — l'occupation
 * vit dans {@link OceanBoard}, comme pour les sites de conservation.
 *
 * <p>La plupart des sites n'ont aucun gain propre ; un seul du matériel en a
 * (deux sites de la tuile de scénario {@code fallen-star}) — la publication y
 * encaisse alors ce petit bonus, en plus des gains de la revue elle-même.
 *
 * @param id           l'identifiant du site sur sa tuile (unique par tuile)
 * @param fieldSymbol  le symbole de domaine requis pour y publier
 * @param gains        les gains propres au site, encaissés à la pose du disque (souvent vide)
 */
public record JournalSite(String id, FieldSymbol fieldSymbol, List<Gain> gains) {

    public JournalSite {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un site de publication doit avoir un identifiant");
        }
        if (fieldSymbol == null) {
            throw new IllegalArgumentException("Le site de publication " + id + " doit avoir un symbole de domaine");
        }
        gains = List.copyOf(gains);
    }
}
