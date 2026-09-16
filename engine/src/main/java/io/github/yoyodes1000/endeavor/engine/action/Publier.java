package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup Publication : dépenser un disque de transit pour le poser sur le
 * site de publication {@code siteId} de la zone {@code cell} — où le joueur
 * actif a un submersible, et dont le symbole de domaine correspond à l'un de
 * ceux de la revue {@code journalId} — après avoir payé son coût en recherche.
 * Le driver paie le coût, pose le disque, encaisse les gains du site et de la
 * revue, puis retire la revue du marché à l'étude.
 *
 * @param journalId l'identifiant de la revue à l'étude visée
 * @param cell      la zone dont on vise un site de publication
 * @param siteId    l'identifiant du site sur la tuile de la zone
 */
public record Publier(String journalId, Cell cell, String siteId) implements Action {

    public Publier {
        if (journalId == null || journalId.isBlank()) {
            throw new IllegalArgumentException("Une Publication vise une revue à l'étude identifiée");
        }
        if (cell == null) {
            throw new IllegalArgumentException("Une Publication vise une zone");
        }
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Une Publication vise un site de publication identifié");
        }
    }
}
