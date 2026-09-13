package io.github.yoyodes1000.endeavor.engine.game;

import java.util.Set;

/**
 * La position de la partie <strong>à l'intérieur de la Phase 2 (Activation)</strong> :
 * le joueur dont c'est le tour et l'ensemble de ceux qui ont déjà passé.
 *
 * <p>Fait partie de l'état (déc. 2) et se copie avec lui (déc. 3) — record immuable
 * (l'ensemble {@code passed} est recopié), remplacé à chaque transition. Le driver
 * d'activation le lit pour exposer les coups légaux et le fait avancer.
 *
 * <p>Version minimale de la structure du tour : elle ne porte encore que le
 * round-robin (qui joue, qui a passé). Le contexte fin d'un tour — spécialiste
 * activé, chaîne d'actions en cours, main de jetons — s'y ajoutera avec la
 * structure complète du tour.
 *
 * @param turnPosition      rang du joueur courant dans l'ordre du tour (0-based)
 * @param passed            indices des joueurs sortis de la manche (ont passé)
 * @param activatedThisTurn vrai si le joueur courant a déjà activé un spécialiste
 *                          ce tour (un seul par tour)
 */
public record ActivationCursor(int turnPosition, Set<Integer> passed, boolean activatedThisTurn) {

    public ActivationCursor {
        if (turnPosition < 0) {
            throw new IllegalArgumentException("La position du tour ne peut être négative");
        }
        if (passed == null) {
            throw new IllegalArgumentException("L'ensemble des joueurs ayant passé est requis");
        }
        passed = Set.copyOf(passed);
    }

    /** Le curseur d'entrée en Phase 2 : premier joueur du tour, personne n'a passé. */
    public static ActivationCursor notStarted() {
        return new ActivationCursor(0, Set.of(), false);
    }
}
