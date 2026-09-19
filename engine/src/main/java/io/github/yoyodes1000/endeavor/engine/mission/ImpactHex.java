package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;
import java.util.Optional;

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
 * <p>Un hexagone peut porter un <strong>symbole de domaine</strong> : il ne rapporte
 * rien à la pose, il se collectionne pour le décompte final. Soit une couleur
 * ({@code fieldSymbol}), soit un joker ({@code wild}) qui vaut n'importe laquelle ;
 * {@code fieldSymbolCount} vaut 2 pour un symbole double.
 *
 * <p>Les autres marqueurs de scénario purement descriptifs ({@code arrow},
 * {@code rescue}, {@code goal}…) ne sont pas encore portés : le chargeur les ignore.
 *
 * @param gains            la récompense à la pose (peut être vide : l'hexagone ne vaut
 *                         alors que ses points)
 * @param fieldSymbol      la couleur du symbole de domaine porté, s'il y en a une
 * @param wild             vrai si le symbole est un joker (exclusif avec {@code fieldSymbol})
 * @param fieldSymbolCount le nombre de symboles portés : 0 si aucun, sinon 1 ou 2
 */
public record ImpactHex(
        int row,
        int col,
        int points,
        List<Gain> gains,
        boolean start,
        boolean offGrid,
        boolean unlimitedCapacity,
        Optional<FieldSymbol> fieldSymbol,
        boolean wild,
        int fieldSymbolCount) {

    public ImpactHex {
        gains = List.copyOf(gains);
        fieldSymbol = fieldSymbol == null ? Optional.empty() : fieldSymbol;
        if (wild && fieldSymbol.isPresent()) {
            throw new IllegalArgumentException("Un symbole est soit une couleur, soit un joker, pas les deux");
        }
        boolean hasSymbol = wild || fieldSymbol.isPresent();
        if (hasSymbol ? fieldSymbolCount < 1 || fieldSymbolCount > 2 : fieldSymbolCount != 0) {
            throw new IllegalArgumentException("Nombre de symboles de domaine invalide : " + fieldSymbolCount);
        }
    }

    /** Un hexagone sans symbole de domaine. */
    public ImpactHex(int row, int col, int points, List<Gain> gains, boolean start, boolean offGrid,
                     boolean unlimitedCapacity) {
        this(row, col, points, gains, start, offGrid, unlimitedCapacity, Optional.empty(), false, 0);
    }
}
