package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;

/**
 * Un bonus de leader par couleur de symbole de domaine : les points vont au(x)
 * joueur(s) qui ont le plus de symboles de cette couleur (jokers compris).
 * Les ex æquo en tête touchent chacun le bonus entier.
 */
public record ColorBonus(FieldSymbol color, int points) {

    public ColorBonus {
        if (color == null) {
            throw new IllegalArgumentException("Un bonus par couleur nomme sa couleur");
        }
        if (points < 1) {
            throw new IllegalArgumentException("Points de bonus par couleur invalides : " + points);
        }
    }
}
