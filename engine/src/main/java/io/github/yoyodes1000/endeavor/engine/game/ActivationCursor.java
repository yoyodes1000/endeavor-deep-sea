package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;

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
 * @param pendingImpacts      pions impact gagnés (par un bonus d'arrivée, une
 *                            cascade) restant à poser avant de poursuivre le tour
 * @param pendingPromotions      gains de promotion gagnés restant à résoudre
 *                               (choisir quel Junior promouvoir) avant de
 *                               poursuivre le tour — ne vaut jamais plus de 0
 *                               sans qu'un Junior promouvable soit détenu
 *                               (sinon le gain est perdu aussitôt)
 * @param pendingAnyAttribute    choix d'attribut libre en attente (les 4
 *                               pistes sont toujours valides — jamais résolu
 *                               sans coup)
 * @param pendingLowestAttribute choix d'attribut en attente entre pistes à
 *                               égalité au plus bas niveau — ne vaut jamais
 *                               plus de 0 sans égalité (sinon la piste la
 *                               plus basse est avancée directement)
 * @param pendingDiscovery       la découverte en cours (tuiles piochées à
 *                               départager puis à poser), ou {@code null} hors
 *                               découverte
 * @param pendingTokenAction     l'action accordée par la dépense d'un jeton de
 *                               plongée en cours de résolution (ex. le Sonar
 *                               promis par un jeton), ou {@code null} hors
 *                               dépense de ce type — suspend le tour sur ce
 *                               seul type d'action, hors chaîne du spécialiste
 */
public record ActivationCursor(int turnPosition, Set<Integer> passed, String activatedSpecialist,
                               int actionStep, int pendingImpacts, int pendingPromotions, int pendingAnyAttribute,
                               int pendingLowestAttribute, PendingDiscovery pendingDiscovery,
                               ActionType pendingTokenAction) {

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
        if (pendingImpacts < 0) {
            throw new IllegalArgumentException("Le nombre d'impacts en attente ne peut être négatif");
        }
        if (pendingPromotions < 0) {
            throw new IllegalArgumentException("Le nombre de promotions en attente ne peut être négatif");
        }
        if (pendingAnyAttribute < 0 || pendingLowestAttribute < 0) {
            throw new IllegalArgumentException("Le nombre de choix d'attribut en attente ne peut être négatif");
        }
        passed = Set.copyOf(passed);
    }

    /** Le curseur d'entrée en Phase 2 : premier joueur du tour, personne n'a passé. */
    public static ActivationCursor notStarted() {
        return new ActivationCursor(0, Set.of(), null, 0, 0, 0, 0, 0, null, null);
    }

    /** Vrai si le joueur courant a déjà activé un spécialiste ce tour. */
    public boolean activatedThisTurn() {
        return activatedSpecialist != null;
    }

    /** Vrai si une découverte est en cours et suspend le tour. */
    public boolean discovering() {
        return pendingDiscovery != null;
    }

    /** Vrai si la dépense d'un jeton de plongée a accordé une action encore à jouer. */
    public boolean resolvingTokenAction() {
        return pendingTokenAction != null;
    }
}
