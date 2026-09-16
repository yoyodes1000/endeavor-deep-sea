package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup « poser une tuile de découverte » : placer la tuile gardée
 * ({@link GarderTuile}) sur une case libre valide de sa profondeur — hors niveau
 * 1, sous une zone existante. Le coup ne porte que la case ; l'identité de la
 * tuile posée vit dans le contexte de tour (comme {@code PoserImpact} ne porte que
 * l'hexagone).
 *
 * @param cell la case d'arrivée, libre et valide pour la profondeur de la tuile
 */
public record PoserTuile(Cell cell) implements Action {

    public PoserTuile {
        if (cell == null) {
            throw new IllegalArgumentException("Une pose de tuile vise une case");
        }
    }
}
