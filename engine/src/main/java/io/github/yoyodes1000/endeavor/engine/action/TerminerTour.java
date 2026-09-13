package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de la Phase 2 : terminer son tour, ce qui rend la main tout en
 * <strong>restant dans la manche</strong> — le joueur rejouera ce round.
 *
 * <p>À distinguer de {@link Passer} (quitter la manche définitivement). Un coup
 * sans paramètre, légal seulement quand le joueur a agi ce tour (sinon, seul
 * {@code Passer} a du sens).
 */
public record TerminerTour() implements Action {
}
