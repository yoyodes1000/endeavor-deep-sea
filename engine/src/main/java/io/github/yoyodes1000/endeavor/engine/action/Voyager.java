package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup Voyage : déplacer un submersible du joueur actif d'une zone
 * {@code from} vers une zone {@code to} accessible. La légalité du trajet
 * (portée et profondeur bornées par le niveau de technologie, vide
 * infranchissable) est établie par {@code OceanBoard.reachableFrom} au moment de
 * lister les coups ; le coup ne porte que le départ et l'arrivée.
 *
 * @param from la zone de départ, où le joueur a un submersible
 * @param to   la zone d'arrivée, accessible et distincte du départ
 */
public record Voyager(Cell from, Cell to) implements Action {

    public Voyager {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Un Voyage a un départ et une arrivée");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Un Voyage doit changer de zone : " + from);
        }
    }
}
