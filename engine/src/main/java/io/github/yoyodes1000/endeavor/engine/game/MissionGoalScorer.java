package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.MajorityBonus;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;

import java.util.List;

/**
 * Le calcul d'un objectif de fin de mission {@link MissionGoal.Standard} :
 * l'effectif (unités comptées, filtrées par profondeur/colonne, ou zones
 * qualifiées par {@code zoneContains}) et le score qui en découle — points par
 * unité, plus la part du bonus de majorité.
 *
 * <p><strong>Égalité de majorité</strong> : les joueurs à égalité se partagent
 * la somme des tranches de bonus qu'ils occupent ensemble, divisée par leur
 * nombre et arrondie à l'inférieur (deux ex æquo en tête se partagent
 * {@code (first + second) / 2} chacun). Une décision du domaine, pas une
 * déduction du code — à vérifier contre le livret de règles si un doute
 * subsiste. Un joueur à effectif nul ne touche aucun bonus, même à égalité
 * avec d'autres joueurs à zéro.
 *
 * <p>{@link MissionGoal.Unsupported} n'est pas accepté ici : l'appelant doit
 * trier les objectifs modélisés des autres (carte sœur de l'orchestration).
 */
public final class MissionGoalScorer {

    private MissionGoalScorer() {
    }

    /** L'effectif du joueur sur cet objectif, avant application de {@code pointsPer}. */
    public static int effectif(MissionGoal.Standard goal, GameState state, int playerIndex) {
        OceanBoard board = state.oceanBoard();
        OceanTileCatalog tiles = state.oceanTileCatalog();
        if (goal.units().contains(GoalUnit.ZONE)) {
            return countZones(goal, board, tiles, playerIndex);
        }
        return countUnits(goal, board, tiles, playerIndex);
    }

    /** Les points du joueur sur cet objectif : {@code pointsPer × effectif}, plus le bonus de majorité. */
    public static int score(MissionGoal.Standard goal, GameState state, int playerIndex) {
        int effectif = effectif(goal, state, playerIndex);
        int base = goal.pointsPer() * effectif;
        int bonus = goal.majorityBonus()
                .map(majorityBonus -> shareOfMajorityBonus(goal, state, playerIndex, effectif, majorityBonus))
                .orElse(0);
        return base + bonus;
    }

    private static int shareOfMajorityBonus(MissionGoal.Standard goal, GameState state, int playerIndex,
                                            int myEffectif, MajorityBonus majorityBonus) {
        if (myEffectif == 0) {
            return 0;
        }
        List<Integer> tiers = List.of(majorityBonus.first(), majorityBonus.second());
        int strictlyGreater = 0;
        int tied = 0;
        for (int p = 0; p < state.playerCount(); p++) {
            int other = p == playerIndex ? myEffectif : effectif(goal, state, p);
            if (other > myEffectif) {
                strictlyGreater++;
            } else if (other == myEffectif) {
                tied++;
            }
        }
        int sum = 0;
        for (int i = 0; i < tied; i++) {
            int position = strictlyGreater + i;
            sum += position < tiers.size() ? tiers.get(position) : 0;
        }
        return sum / tied;
    }

    private static int countUnits(MissionGoal.Standard goal, OceanBoard board, OceanTileCatalog tiles,
                                  int playerIndex) {
        int total = 0;
        for (Cell cell : board.occupiedCells()) {
            if (!matchesFilter(goal, cell)) {
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
                                  int playerIndex) {
        int zones = 0;
        for (Cell cell : board.occupiedCells()) {
            if (!matchesFilter(goal, cell)) {
                continue;
            }
            OceanTile tile = OceanOwnership.tileAt(board, tiles, cell);
            boolean qualifies = goal.zoneContains().stream()
                    .anyMatch(unit -> countUnitInCell(unit, board, tile, cell, playerIndex) > 0);
            if (qualifies) {
                zones++;
            }
        }
        return zones;
    }

    private static boolean matchesFilter(MissionGoal.Standard goal, Cell cell) {
        boolean depthOk = goal.depths().isEmpty() || goal.depths().contains(cell.depth());
        boolean columnOk = goal.columns().isEmpty() || goal.columns().contains(cell.col());
        return depthOk && columnOk;
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
