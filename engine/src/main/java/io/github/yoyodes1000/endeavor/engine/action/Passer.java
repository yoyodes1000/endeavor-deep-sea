package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de la Phase 2 : passer, ce qui clôt le tour du joueur et le sort de la
 * manche <strong>définitivement</strong> — il ne rejouera plus cette Phase 2.
 *
 * <p>À distinguer de « terminer le tour » (rendre la main tout en restant dans la
 * manche), qui viendra avec la structure complète du tour. Un coup sans paramètre.
 */
public record Passer() implements Action {
}
