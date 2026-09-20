package io.github.yoyodes1000.endeavor.engine.mission;

import java.util.List;

/**
 * Un bonus de leader d'un objectif : les points vont au(x) joueur(s) qui ont le plus
 * de ce que compte l'objectif, dans les profondeurs et colonnes données (vide = toutes).
 * Les ex æquo en tête touchent chacun le bonus entier.
 *
 * @param depths  les profondeurs où l'on compte, {@code []} = toutes
 * @param columns les colonnes où l'on compte (indices 0-based), {@code []} = toutes
 * @param points  le bonus, strictement positif
 */
public record LeaderBonus(List<Integer> depths, List<Integer> columns, int points) {

    public LeaderBonus {
        depths = List.copyOf(depths == null ? List.of() : depths);
        columns = List.copyOf(columns == null ? List.of() : columns);
        if (depths.isEmpty() && columns.isEmpty()) {
            throw new IllegalArgumentException("Un bonus de leader cible une profondeur ou une colonne");
        }
        if (points < 1) {
            throw new IllegalArgumentException("Points de bonus de leader invalides : " + points);
        }
    }
}
