package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.engine.action.Action;

/**
 * Un coup du joueur humain.
 *
 * @param moveNumber le numéro de coup lu dans la vue sur laquelle le coup a été choisi
 * @param action     le coup, au format JSON des coups
 *                   ({@link io.github.yoyodes1000.endeavor.app.json.ActionJsonModule})
 */
public record MoveRequest(Integer moveNumber, Action action) {
}
