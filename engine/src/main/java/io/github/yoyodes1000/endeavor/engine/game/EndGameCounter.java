package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.JournalSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameCount;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Les huit prédicats d'effectif des décomptes de fin de partie Senior
 * (décision 6 de l'architecture : l'arithmétique est en données dans
 * {@link io.github.yoyodes1000.endeavor.engine.specialist.EndGameScoring},
 * le prédicat est ici, en code).
 *
 * <p>Deux effectifs restent hors de portée pour l'instant :
 * {@link EndGameCount#COMPLETE_FIELD_SYMBOL_SETS} se compte sur les
 * symboles de domaine des hexagones du plateau Impact occupés — champ pas
 * encore porté par {@code ImpactHex} (carte sœur) — et
 * {@link EndGameCount#CONNECTIONS_SHARED_WITH_OPPONENTS} sur les connexions
 * entre sites d'une même tuile, une mécanique que {@code OceanTile} ne
 * modélise pas encore. Les deux échouent bruyamment plutôt que de rendre un
 * effectif de zéro qui fausserait silencieusement un score.
 */
public final class EndGameCounter {

    private EndGameCounter() {
    }

    /**
     * L'effectif compté sur l'état de la partie pour ce décompte, du point de
     * vue du joueur d'indice {@code playerIndex}.
     *
     * @param specialistId l'identifiant de la tuile qui porte ce décompte —
     *                     exclue d'elle-même pour {@link EndGameCount#OTHER_SENIOR_SPECIALISTS}
     */
    public static int effectif(EndGameCount count, GameState state, int playerIndex, String specialistId) {
        return switch (count) {
            case UNCONDITIONAL -> 1;
            case OTHER_SENIOR_SPECIALISTS -> otherSeniorSpecialists(state.player(playerIndex), specialistId);
            case EMPTY_DIVE_SITES -> emptyDiveSites(state.oceanBoard(), state.oceanTileCatalog());
            case CONSERVATION_DISCS_IN_BEST_ZONE ->
                    conservationDiscsInBestZone(state.oceanBoard(), state.oceanTileCatalog(), playerIndex);
            case ZONES_WITH_CONSERVATION ->
                    zonesWithConservation(state.oceanBoard(), state.oceanTileCatalog(), playerIndex);
            case DEPTHS_WITH_PUBLICATION ->
                    depthsWithPublication(state.oceanBoard(), state.oceanTileCatalog(), playerIndex);
            case ZONES_WITH_DISC_OR_VESSEL ->
                    zonesWithDiscOrVessel(state.oceanBoard(), state.oceanTileCatalog(), playerIndex);
            case COMPLETE_FIELD_SYMBOL_SETS -> throw notYetImplemented(count);
            case CONNECTIONS_SHARED_WITH_OPPONENTS -> throw notYetImplemented(count);
        };
    }

    private static UnsupportedOperationException notYetImplemented(EndGameCount count) {
        return new UnsupportedOperationException(
                "Effectif pas encore câblé : " + count + " (carte sœur du décompte final)");
    }

    private static int otherSeniorSpecialists(Player player, String specialistId) {
        int count = 0;
        for (HeldSpecialist held : player.specialists()) {
            if (held.face() == SpecialistFace.SENIOR && !held.specialist().id().equals(specialistId)) {
                count++;
            }
        }
        return count;
    }

    private static int emptyDiveSites(OceanBoard board, OceanTileCatalog tiles) {
        int empty = 0;
        for (Cell cell : board.occupiedCells()) {
            for (DiveSite site : tileAt(board, tiles, cell).diveSites()) {
                if (board.diveTokenCount(cell, site.id()) == 0) {
                    empty++;
                }
            }
        }
        return empty;
    }

    private static int conservationDiscsInBestZone(OceanBoard board, OceanTileCatalog tiles, int playerIndex) {
        int best = 0;
        for (Cell cell : board.occupiedCells()) {
            int here = 0;
            for (ConservationSite site : tileAt(board, tiles, cell).conservationSites()) {
                if (isOwnedBy(board.conservationOccupant(cell, site.id()), playerIndex)) {
                    here++;
                }
            }
            best = Math.max(best, here);
        }
        return best;
    }

    private static int zonesWithConservation(OceanBoard board, OceanTileCatalog tiles, int playerIndex) {
        int zones = 0;
        for (Cell cell : board.occupiedCells()) {
            boolean own = tileAt(board, tiles, cell).conservationSites().stream()
                    .anyMatch(site -> isOwnedBy(board.conservationOccupant(cell, site.id()), playerIndex));
            if (own) {
                zones++;
            }
        }
        return zones;
    }

    private static int depthsWithPublication(OceanBoard board, OceanTileCatalog tiles, int playerIndex) {
        Set<Integer> depths = new HashSet<>();
        for (Cell cell : board.occupiedCells()) {
            boolean own = tileAt(board, tiles, cell).journalSites().stream()
                    .anyMatch(site -> isOwnedBy(board.journalOccupant(cell, site.id()), playerIndex));
            if (own) {
                depths.add(cell.depth());
            }
        }
        return depths.size();
    }

    private static int zonesWithDiscOrVessel(OceanBoard board, OceanTileCatalog tiles, int playerIndex) {
        int zones = 0;
        for (Cell cell : board.occupiedCells()) {
            if (board.vesselCount(cell, playerIndex) > 0 || hasAnyDisc(board, tileAt(board, tiles, cell), cell, playerIndex)) {
                zones++;
            }
        }
        return zones;
    }

    private static boolean hasAnyDisc(OceanBoard board, OceanTile tile, Cell cell, int playerIndex) {
        for (int trackIndex = 0; trackIndex < tile.sonarTracks().size(); trackIndex++) {
            if (board.sonarDiscOwners(cell, trackIndex).contains(playerIndex)) {
                return true;
            }
        }
        boolean hasConservation = tile.conservationSites().stream()
                .anyMatch(site -> isOwnedBy(board.conservationOccupant(cell, site.id()), playerIndex));
        boolean hasPublication = tile.journalSites().stream()
                .anyMatch(site -> isOwnedBy(board.journalOccupant(cell, site.id()), playerIndex));
        return hasConservation || hasPublication;
    }

    private static boolean isOwnedBy(Optional<Integer> occupant, int playerIndex) {
        return occupant.isPresent() && occupant.get() == playerIndex;
    }

    private static OceanTile tileAt(OceanBoard board, OceanTileCatalog tiles, Cell cell) {
        String tileId = board.tileAt(cell)
                .orElseThrow(() -> new IllegalStateException("Case occupée sans tuile : " + cell));
        return tiles.byId(tileId)
                .orElseThrow(() -> new IllegalStateException("Tuile inconnue du catalogue : " + tileId));
    }
}
