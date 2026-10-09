package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.engine.action.Action;

/**
 * Un coup joué, avec le siège qui l'a joué.
 *
 * @param seat   le siège du joueur
 * @param action le coup
 */
public record PlayedMove(int seat, Action action) {
}
