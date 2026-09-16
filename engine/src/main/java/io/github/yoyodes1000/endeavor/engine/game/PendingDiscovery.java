package io.github.yoyodes1000.endeavor.engine.game;

import java.util.List;

/**
 * L'état d'une <strong>découverte en cours</strong>, suspendue au milieu d'un
 * Sonar : les deux tuiles piochées tant qu'aucune n'est choisie, puis la tuile
 * gardée tant qu'elle n'est pas posée. Porté par l'{@link ActivationCursor} —
 * immuable, copié avec l'état (déc. 2 et 3), à l'image de {@code pendingImpacts}.
 *
 * <p>Deux temps : <em>choix</em> ({@code keptTile} nul, {@code candidates} =
 * les tuiles à départager) puis <em>pose</em> ({@code keptTile} fixé). La
 * transition passe par {@link #kept(String)}.
 *
 * @param candidates les tuiles piochées à départager (vide une fois le choix fait)
 * @param keptTile   la tuile conservée, ou {@code null} tant que le choix n'est pas fait
 */
public record PendingDiscovery(List<String> candidates, String keptTile) {

    public PendingDiscovery {
        candidates = List.copyOf(candidates);
        if (keptTile == null && candidates.isEmpty()) {
            throw new IllegalArgumentException("Une découverte à départager doit avoir des tuiles candidates");
        }
    }

    /** Une découverte au temps du choix, avec les tuiles piochées à départager. */
    public static PendingDiscovery toChooseFrom(List<String> candidates) {
        return new PendingDiscovery(candidates, null);
    }

    /** La même découverte au temps de la pose, une fois {@code tileId} conservé. */
    public PendingDiscovery kept(String tileId) {
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("La tuile gardée doit avoir un identifiant");
        }
        return new PendingDiscovery(List.of(), tileId);
    }

    /** Vrai tant que le joueur doit choisir laquelle des tuiles piochées garder. */
    public boolean awaitingChoice() {
        return keptTile == null;
    }
}
