package io.github.yoyodes1000.endeavor.engine.game;

/**
 * La position de la partie <strong>à l'intérieur de la Phase 1 (Préparation)</strong> :
 * quel joueur agit, à quelle étape, et ce qu'il reste à jouer d'une cascade en cours.
 *
 * <p>Fait partie de l'état (déc. 2) et se copie avec lui (déc. 3) — c'est un record
 * immuable, remplacé à chaque transition, jamais muté. Le driver de préparation le
 * lit pour exposer les coups légaux et le fait avancer en appliquant les coups.
 *
 * <p>Seules les étapes de <strong>décision</strong> et l'entrée/sortie de phase y
 * figurent ({@code RECRUIT}, {@code PLACE_IMPACT}, {@code RECOVER}, plus
 * {@code NOT_STARTED} et {@code DONE}). Les étapes automatiques (premier joueur,
 * effort) ne sont pas des points d'arrêt : le driver les traverse sans s'y poser.
 *
 * @param turnPosition       rang du joueur courant dans l'ordre du tour (0-based)
 * @param step               l'étape de décision courante
 * @param pendingImpacts     pions impact restant à poser (cascade en cours)
 * @param pendingVessels     submersibles gagnés, en attente de mise en jeu (différée)
 * @param remainingRecoveries disques encore récupérables ce tour (budget 1c restant)
 */
public record PreparationCursor(
        int turnPosition,
        Step step,
        int pendingImpacts,
        int pendingVessels,
        int remainingRecoveries) {

    /** Les étapes où la partie s'arrête (ou marque l'entrée/sortie de la phase). */
    public enum Step {
        /** La phase n'a pas encore été entamée : appeler {@code begin} d'abord. */
        NOT_STARTED,
        /** 1a : le joueur choisit une tuile à recruter. */
        RECRUIT,
        /** Cascade : le joueur pose un pion impact gagné. */
        PLACE_IMPACT,
        /** 1c : le joueur choisit sur quel spécialiste reprendre un disque. */
        RECOVER,
        /** La Phase 1 de la manche est terminée. */
        DONE
    }

    public PreparationCursor {
        if (step == null) {
            throw new IllegalArgumentException("Le curseur a une étape");
        }
        if (turnPosition < 0 || pendingImpacts < 0 || pendingVessels < 0 || remainingRecoveries < 0) {
            throw new IllegalArgumentException("Les compteurs du curseur ne peuvent être négatifs");
        }
    }

    /** Le curseur d'une partie neuve : la Phase 1 n'est pas encore entamée. */
    public static PreparationCursor notStarted() {
        return new PreparationCursor(0, Step.NOT_STARTED, 0, 0, 0);
    }
}
