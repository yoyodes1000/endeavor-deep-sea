package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.List;

/**
 * Une piste Sonar d'une tuile : une suite ordonnée de {@link SonarSpot}, remplie
 * de gauche à droite. Une tuile peut en porter plusieurs (une {@code List} sur
 * {@link OceanTile}).
 *
 * <p>Matériel immuable : la piste décrit les cases ; l'occupation par des disques
 * — donc la case libre la plus à gauche — vit dans {@link OceanBoard}, mutable.
 *
 * @param spots les cases dans l'ordre de lecture (gauche → droite), au moins une
 */
public record SonarTrack(List<SonarSpot> spots) {

    public SonarTrack {
        if (spots == null || spots.isEmpty()) {
            throw new IllegalArgumentException("Une piste Sonar doit avoir au moins une case");
        }
        spots = List.copyOf(spots);
    }
}
