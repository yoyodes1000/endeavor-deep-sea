package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Le coup « garder une tuile » : après un Sonar qui a fait piocher deux tuiles de
 * découverte, choisir celle que l'on conserve — l'autre retourne sous sa pile. Le
 * coup porte l'identifiant de la tuile gardée, choisi parmi les deux candidates
 * que le driver a révélées.
 *
 * @param tileId l'identifiant de la tuile conservée
 */
public record GarderTuile(String tileId) implements Action {

    public GarderTuile {
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("Une tuile gardée doit avoir un identifiant");
        }
    }
}
