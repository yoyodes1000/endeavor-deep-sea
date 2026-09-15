package io.github.yoyodes1000.endeavor.engine.action;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;

/**
 * Le coup Sonar : poser un disque sur la case libre la plus à gauche d'une piste
 * Sonar de la zone {@code cell}, où le joueur actif a un submersible. La case
 * étant forcée (la plus à gauche), le coup ne désigne que la <strong>piste</strong>
 * — sa zone et son rang sur la tuile ; le driver résout la case et son effet
 * (récompense pour l'instant ; découverte à venir).
 *
 * @param cell       la zone dont on vise une piste Sonar
 * @param trackIndex le rang de la piste sur la tuile (0-based)
 */
public record Sonar(Cell cell, int trackIndex) implements Action {

    public Sonar {
        if (cell == null) {
            throw new IllegalArgumentException("Un Sonar vise une zone");
        }
        if (trackIndex < 0) {
            throw new IllegalArgumentException("Rang de piste Sonar négatif : " + trackIndex);
        }
    }
}
