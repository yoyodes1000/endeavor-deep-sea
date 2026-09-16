package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;

/**
 * Le coup Choix d'attribut : avance la piste {@code attribute} d'un cran, pour
 * résoudre un gain {@code anyAttribute} (choix libre parmi les 4) ou
 * {@code lowestAttribute} en attente (choix entre pistes à égalité au plus bas
 * niveau — sans égalité, {@link
 * io.github.yoyodes1000.endeavor.engine.effect.GainResolver} l'a déjà résolu
 * sans coup).
 *
 * @param attribute la piste choisie
 */
public record ChoisirAttribut(Attribute attribute) implements Action {

    public ChoisirAttribut {
        if (attribute == null) {
            throw new IllegalArgumentException("Un choix d'attribut vise une piste identifiée");
        }
    }
}
