package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de la cascade d'effets : poser un pion impact sur l'hexagone du plateau de
 * mission repéré par ses coordonnées.
 *
 * <p>L'hexagone est désigné par sa position (row/col), une donnée sérialisable :
 * le driver la résout en {@code ImpactHex} sur le plateau courant, et vérifie sa
 * légalité (case libre, de départ ou voisine d'une occupée). Le coup ne porte donc
 * pas l'objet hexagone, seulement de quoi le retrouver.
 *
 * @param row la ligne de l'hexagone visé
 * @param col la colonne de l'hexagone visé
 */
public record PoserImpact(int row, int col) implements Action {
}
