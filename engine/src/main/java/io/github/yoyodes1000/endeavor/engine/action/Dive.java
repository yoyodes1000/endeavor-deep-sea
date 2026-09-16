package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup Plongée : prendre le jeton du sommet d'un site de plongée de la zone
 * {@code cell}, où le joueur actif a un submersible. Sans coût — contrairement
 * au Sonar, aucun disque n'est dépensé. Le jeton pris rejoint la main du joueur,
 * non résolu ({@link DepenserJeton} en choisit l'option plus tard).
 *
 * @param cell   la zone dont on vise un site de plongée
 * @param siteId l'identifiant du site sur la tuile de la zone
 */
public record Dive(Cell cell, String siteId) implements Action {

    public Dive {
        if (cell == null) {
            throw new IllegalArgumentException("Une Plongée vise une zone");
        }
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Une Plongée vise un site de plongée identifié");
        }
    }
}
