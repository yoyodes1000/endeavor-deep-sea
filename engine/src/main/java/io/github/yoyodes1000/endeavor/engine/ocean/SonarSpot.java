package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Une case d'une {@link SonarTrack} : le disque de Sonar se pose sur la case
 * libre la plus à gauche, et son type détermine l'effet. Deux formes —
 * <strong>récompense</strong> (encaisser des gains) ou <strong>découverte</strong>
 * (piocher et poser une nouvelle tuile parmi les niveaux indiqués).
 *
 * <p>Interface scellée : le traitement des deux cas est exhaustif et vérifié par
 * le compilateur, à l'image de {@link StartingTile}.
 */
public sealed interface SonarSpot permits SonarSpot.Reward, SonarSpot.Discover {

    /** Une case qui accorde des gains à la pose d'un disque. */
    record Reward(List<Gain> gains) implements SonarSpot {
        public Reward {
            gains = List.copyOf(gains);
        }
    }

    /**
     * Une case qui déclenche une découverte : piocher parmi les niveaux de
     * profondeur indiqués (l'icône « Découvrir »).
     */
    record Discover(List<Integer> levels) implements SonarSpot {
        public Discover {
            if (levels == null || levels.isEmpty()) {
                throw new IllegalArgumentException("Une case de découverte doit indiquer au moins un niveau");
            }
            for (int level : levels) {
                if (level < 1 || level > 5) {
                    throw new IllegalArgumentException("Niveau de découverte hors de 1..5 : " + level);
                }
            }
            levels = List.copyOf(levels);
        }
    }
}
