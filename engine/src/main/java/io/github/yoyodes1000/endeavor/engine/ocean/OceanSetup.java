package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.List;

/**
 * La mise en place de l'océan d'une mission (décision 8, bloc {@code setup}) : le
 * nombre de colonnes et les tuiles posées d'entrée.
 *
 * <p>Les cas d'information cachée des scénarios tardifs ({@code shuffledRows},
 * {@code hiddenTiles}) et la base d'opérations viendront s'ajouter quand leurs
 * missions seront traitées ; on ne modélise ici que ce dont la mission 1 a besoin.
 */
public record OceanSetup(int columns, List<StartingTile> startingTiles) {

    public OceanSetup {
        if (columns < 1) {
            throw new IllegalArgumentException("Un océan a au moins une colonne : " + columns);
        }
        if (startingTiles == null) {
            throw new IllegalArgumentException("La mise en place doit lister ses tuiles de départ");
        }
        startingTiles = List.copyOf(startingTiles);
    }
}
