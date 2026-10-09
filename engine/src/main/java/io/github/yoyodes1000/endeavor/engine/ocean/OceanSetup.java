package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.List;

/**
 * La mise en place de l'océan d'une mission (décision 8, bloc {@code setup}) : le
 * nombre de colonnes, les tuiles posées d'entrée, les lignes mélangées, les tuiles
 * cachées dans la pioche et la profondeur maximale de la mission.
 *
 * <p>Une mission à profondeur maximale réduite (par exemple 3) retire du jeu les
 * tuiles plus profondes : la pioche de découverte n'en contient plus.
 *
 * @param shuffledRows lignes dont les tuiles sont réparties au hasard entre les colonnes
 * @param hiddenTiles  tuiles glissées dans la pioche de découverte
 * @param maxDepth     la profondeur maximale de la mission, de 1 à {@link #DEEPEST}
 */
public record OceanSetup(int columns, List<StartingTile> startingTiles, List<ShuffledRow> shuffledRows,
                         List<HiddenTile> hiddenTiles, int maxDepth) {

    /** La plus grande profondeur du matériel. */
    public static final int DEEPEST = 5;

    public OceanSetup {
        if (columns < 1) {
            throw new IllegalArgumentException("Un océan a au moins une colonne : " + columns);
        }
        if (startingTiles == null) {
            throw new IllegalArgumentException("La mise en place doit lister ses tuiles de départ");
        }
        if (maxDepth < 1 || maxDepth > DEEPEST) {
            throw new IllegalArgumentException("Profondeur maximale hors de 1..5 : " + maxDepth);
        }
        startingTiles = List.copyOf(startingTiles);
        shuffledRows = List.copyOf(shuffledRows == null ? List.of() : shuffledRows);
        hiddenTiles = List.copyOf(hiddenTiles == null ? List.of() : hiddenTiles);
    }

    /** Une mise en place sans ligne mélangée ni tuile cachée, à pleine profondeur. */
    public OceanSetup(int columns, List<StartingTile> startingTiles) {
        this(columns, startingTiles, List.of(), List.of(), DEEPEST);
    }
}
