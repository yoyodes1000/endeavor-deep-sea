package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup Conservation : dépenser un disque de transit pour le poser sur le
 * site de conservation {@code siteId} de la zone {@code cell}, où le joueur
 * actif a un submersible — après avoir payé le coût en recherche du site. Le
 * driver paie le coût, pose le disque, et encaisse les gains du site.
 *
 * @param cell   la zone dont on vise un site de conservation
 * @param siteId l'identifiant du site sur la tuile de la zone
 */
public record Conserver(Cell cell, String siteId) implements Action {

    public Conserver {
        if (cell == null) {
            throw new IllegalArgumentException("Une Conservation vise une zone");
        }
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Une Conservation vise un site de conservation identifié");
        }
    }
}
