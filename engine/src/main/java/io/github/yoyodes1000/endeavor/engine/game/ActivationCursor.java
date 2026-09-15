package io.github.yoyodes1000.endeavor.engine.game;

import java.util.Set;

/**
 * La position de la partie <strong>à l'intérieur de la Phase 2 (Activation)</strong> :
 * le joueur dont c'est le tour, ceux qui ont déjà passé, et — pendant un tour — le
 * spécialiste activé et l'avancement dans sa chaîne d'actions.
 *
 * <p>Fait partie de l'état (déc. 2) et se copie avec lui (déc. 3) — record immuable
 * (l'ensemble {@code passed} est recopié), remplacé à chaque transition. Le driver
 * d'activation le lit pour exposer les coups légaux et le fait avancer.
 *
 * @param turnPosition        rang du joueur courant dans l'ordre du tour (0-based)
 * @param passed              indices des joueurs sortis de la manche (ont passé)
 * @param activatedSpecialist identifiant du spécialiste activé ce tour, ou
 *                            {@code null} tant qu'aucun ne l'est (un seul par tour)
 * @param actionStep          index de l'emplacement d'action courant dans la chaîne
 *                            du spécialiste activé (0 juste après l'activation)
 */
public record ActivationCursor(int turnPosition, Set<Integer> passed, String activatedSpecialist, int actionStep) {

    public ActivationCursor {
        if (turnPosition < 0) {
            throw new IllegalArgumentException("La position du tour ne peut être négative");
        }
        if (passed == null) {
            throw new IllegalArgumentException("L'ensemble des joueurs ayant passé est requis");
        }
        if (actionStep < 0) {
            throw new IllegalArgumentException("L'avancement dans la chaîne ne peut être négatif");
        }
        passed = Set.copyOf(passed);
    }

    /** Le curseur d'entrée en Phase 2 : premier joueur du tour, personne n'a passé. */
    public static ActivationCursor notStarted() {
        return new ActivationCursor(0, Set.of(), null, 0);
    }

    /** Vrai si le joueur courant a déjà activé un spécialiste ce tour. */
    public boolean activatedThisTurn() {
        return activatedSpecialist != null;
    }
}
