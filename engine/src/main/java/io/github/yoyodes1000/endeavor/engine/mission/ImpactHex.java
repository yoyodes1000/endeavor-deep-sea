package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Un hexagone du plateau Impact : sa position (row/col), les points qu'il vaut au
 * décompte final, la récompense immédiate accordée quand on y pose un pion, et
 * quelques marqueurs.
 *
 * <p>Les points peuvent être <strong>négatifs</strong> (hexagones pollués de la
 * mission 9). {@code offGrid} marque les hexagones ∞ hors-grille (atteints par
 * une flèche, pas par adjacence géométrique) ; {@code unlimitedCapacity} indique
 * qu'ils accueillent plusieurs pions.
 *
 * <p>Les marqueurs de scénario purement descriptifs ({@code fieldSymbol},
 * {@code arrow}, {@code rescue}, {@code goal}…) ne sont pas encore portés : ils
 * relèvent du décompte / de la Phase 2 et le chargeur les ignore pour l'instant.
 *
 * @param gains la récompense à la pose (peut être vide : l'hexagone ne vaut alors
 *              que ses points)
 */
public record ImpactHex(
        int row,
        int col,
        int points,
        List<Gain> gains,
        boolean start,
        boolean offGrid,
        boolean unlimitedCapacity) {

    public ImpactHex {
        gains = List.copyOf(gains);
    }
}
