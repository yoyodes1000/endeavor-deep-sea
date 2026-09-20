package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.mission.ColorBonus;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.LeaderBonus;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Le calcul d'un objectif de fin de mission {@link MissionGoal.Standard} :
 * l'effectif (unités comptées, filtrées par profondeur/colonne, ou zones
 * qualifiées par {@code zoneContains} ou par leur découverte) et le score qui en
 * découle — points par unité, plus le bonus de majorité, les bonus de leader et
 * les bonus par couleur.
 *
 * <p>La répartition des bonus entre ex æquo est décrite par {@link MajorityShare}.
 *
 * <p>L'unité {@code fieldSymbol} compte les symboles de la couleur que le joueur
 * possède le plus (jokers ajoutés à celle-ci) ; les filtres de profondeur et de
 * colonne ne s'y appliquent pas.
 *
 * <p>{@link MissionGoal.Unsupported} n'est pas accepté ici : l'appelant doit
 * trier les objectifs modélisés des autres.
 */
public final class MissionGoalScorer {

    private static final Filter NO_EXTRA_FILTER = new Filter(List.of(), List.of());

    private MissionGoalScorer() {
    }

    /** Un filtre de profondeurs et de colonnes ; une liste vide accepte tout. */
    private record Filter(List<Integer> depths, List<Integer> columns) {

        boolean accepts(Cell cell) {
            return (depths.isEmpty() || depths.contains(cell.depth()))
                    && (columns.isEmpty() || columns.contains(cell.col()));
        }
    }

    /** L'effectif du joueur sur cet objectif, avant application de {@code pointsPer}. */
    public static int effectif(MissionGoal.Standard goal, GameState state, int playerIndex) {
        return effectif(goal, state, playerIndex, NO_EXTRA_FILTER);
    }

    /** Les points du joueur sur cet objectif : {@code pointsPer × effectif}, plus tous les bonus. */
    public static int score(MissionGoal.Standard goal, GameState state, int playerIndex) {
        int[] effectifs = forEachPlayer(state, player -> effectif(goal, state, player));
        int score = goal.pointsPer() * effectifs[playerIndex];
        score += goal.majorityBonus()
                .map(bonus -> MajorityShare.twoTiers(effectifs, playerIndex, bonus.first(), bonus.second()))
                .orElse(0);
        for (LeaderBonus bonus : goal.leaderBonuses()) {
            Filter filter = new Filter(bonus.depths(), bonus.columns());
            int[] inScope = forEachPlayer(state, player -> effectif(goal, state, player, filter));
            score += MajorityShare.leader(inScope, playerIndex, bonus.points());
        }
        for (ColorBonus bonus : goal.colorBonuses()) {
            int[] ofColor = forEachPlayer(state, player ->
                    FieldSymbolTally.of(state.missionBoard(), player).countOf(bonus.color()));
            score += MajorityShare.leader(ofColor, playerIndex, bonus.points());
        }
        return score;
    }

    private static int[] forEachPlayer(GameState state, IntUnaryOperator perPlayer) {
        int[] values = new int[state.playerCount()];
        for (int player = 0; player < values.length; player++) {
            values[player] = perPlayer.applyAsInt(player);
        }
        return values;
    }

    private static int effectif(MissionGoal.Standard goal, GameState state, int playerIndex, Filter extraFilter) {
        OceanBoard board = state.oceanBoard();
        OceanTileCatalog tiles = state.oceanTileCatalog();
        if (goal.units().contains(GoalUnit.ZONE)) {
            return countZones(goal, board, tiles, playerIndex, extraFilter);
        }
        if (goal.units().contains(GoalUnit.FIELD_SYMBOL)) {
            return FieldSymbolTally.of(state.missionBoard(), playerIndex).mostHeld();
        }
        return countUnits(goal, board, tiles, playerIndex, extraFilter);
    }

    private static int countUnits(MissionGoal.Standard goal, OceanBoard board, OceanTileCatalog tiles,
                                  int playerIndex, Filter extraFilter) {
        int total = 0;
        for (Cell cell : board.occupiedCells()) {
            if (!matchesFilters(goal, extraFilter, cell)) {
                continue;
            }
            OceanTile tile = OceanOwnership.tileAt(board, tiles, cell);
            for (GoalUnit unit : goal.units()) {
                total += countUnitInCell(unit, board, tile, cell, playerIndex);
            }
        }
        return total;
    }

    private static int countZones(MissionGoal.Standard goal, OceanBoard board, OceanTileCatalog tiles,
                                  int playerIndex, Filter extraFilter) {
        int zones = 0;
        for (Cell cell : board.occupiedCells()) {
            if (matchesFilters(goal, extraFilter, cell)
                    && zoneQualifies(goal, board, OceanOwnership.tileAt(board, tiles, cell), cell, playerIndex)) {
                zones++;
            }
        }
        return zones;
    }

    /** Une zone compte si le joueur l'a découverte (quand l'objectif l'exige) et y a l'une des unités demandées. */
    private static boolean zoneQualifies(MissionGoal.Standard goal, OceanBoard board, OceanTile tile, Cell cell,
                                         int playerIndex) {
        if (goal.discoveredByYou() && board.discovererOf(cell).orElse(-1) != playerIndex) {
            return false;
        }
        return goal.zoneContains().isEmpty()
                || goal.zoneContains().stream()
                        .anyMatch(unit -> countUnitInCell(unit, board, tile, cell, playerIndex) > 0);
    }

    private static boolean matchesFilters(MissionGoal.Standard goal, Filter extraFilter, Cell cell) {
        return new Filter(goal.depths(), goal.columns()).accepts(cell) && extraFilter.accepts(cell);
    }

    private static int countUnitInCell(GoalUnit unit, OceanBoard board, OceanTile tile, Cell cell, int playerIndex) {
        return switch (unit) {
            case SONAR -> OceanOwnership.sonarDiscsOwnedBy(board, tile, cell, playerIndex);
            case CONSERVE -> OceanOwnership.conservationDiscsOwnedBy(board, tile, cell, playerIndex);
            case PUBLISH -> OceanOwnership.journalDiscsOwnedBy(board, tile, cell, playerIndex);
            case DISC -> OceanOwnership.sonarDiscsOwnedBy(board, tile, cell, playerIndex)
                    + OceanOwnership.conservationDiscsOwnedBy(board, tile, cell, playerIndex)
                    + OceanOwnership.journalDiscsOwnedBy(board, tile, cell, playerIndex);
            case VESSEL -> board.vesselCount(cell, playerIndex);
            case ZONE, FIELD_SYMBOL, IMPACT_MARKER ->
                    throw new UnsupportedOperationException("Unité pas encore câblée : " + unit);
        };
    }
}
