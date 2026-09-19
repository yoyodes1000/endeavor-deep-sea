package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.JournalSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;

import java.util.Optional;

/**
 * Primitives de comptage « combien de disques de ce joueur dans cette zone »,
 * partagées par {@link EndGameCounter} et {@link MissionGoalScorer} : les deux
 * comptent la même chose (disques Sonar/Conservation/Publication, submersibles)
 * sur les mêmes données ({@link OceanBoard}), seul le filtre qui les agrège
 * diffère.
 */
final class OceanOwnership {

    private OceanOwnership() {
    }

    static OceanTile tileAt(OceanBoard board, OceanTileCatalog tiles, Cell cell) {
        String tileId = board.tileAt(cell)
                .orElseThrow(() -> new IllegalStateException("Case occupée sans tuile : " + cell));
        return tiles.byId(tileId)
                .orElseThrow(() -> new IllegalStateException("Tuile inconnue du catalogue : " + tileId));
    }

    /** Combien de disques de Sonar de ce joueur dans cette zone, toutes pistes confondues. */
    static int sonarDiscsOwnedBy(OceanBoard board, OceanTile tile, Cell cell, int playerIndex) {
        int count = 0;
        for (int trackIndex = 0; trackIndex < tile.sonarTracks().size(); trackIndex++) {
            for (int owner : board.sonarDiscOwners(cell, trackIndex)) {
                if (owner == playerIndex) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Combien de disques de conservation de ce joueur dans cette zone. */
    static int conservationDiscsOwnedBy(OceanBoard board, OceanTile tile, Cell cell, int playerIndex) {
        int count = 0;
        for (ConservationSite site : tile.conservationSites()) {
            if (isOwnedBy(board.conservationOccupant(cell, site.id()), playerIndex)) {
                count++;
            }
        }
        return count;
    }

    /** Combien de disques de publication de ce joueur dans cette zone. */
    static int journalDiscsOwnedBy(OceanBoard board, OceanTile tile, Cell cell, int playerIndex) {
        int count = 0;
        for (JournalSite site : tile.journalSites()) {
            if (isOwnedBy(board.journalOccupant(cell, site.id()), playerIndex)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isOwnedBy(Optional<Integer> occupant, int playerIndex) {
        return occupant.isPresent() && occupant.get() == playerIndex;
    }
}
