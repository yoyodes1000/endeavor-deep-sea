package io.github.yoyodes1000.endeavor.app.mission;

import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;

import java.util.List;
import java.util.Map;

/**
 * Reflet brut de {@code missions.json} : l'identité de la mission, son plateau
 * Impact, la mise en place de l'océan ({@code setup} : colonnes et tuiles de
 * départ) et ses trois objectifs de fin de mission ({@code goals}). Les règles
 * spéciales ne sont pas déclarées : le chargeur les ignore (Phase 2 à venir).
 */
record MissionsDocument(List<Entry> missions) {

    record Entry(String id, Integer number, String name, ImpactBoardDto impactBoard, SetupDto setup,
                 List<GoalDto> goals) {
    }

    record ImpactBoardDto(String orientation, List<HexDto> hexes) {
    }

    record SetupDto(
            Integer columns,
            List<StartingTileDto> startingTiles,
            CellDto baseOfOperations,
            Integer startingVessels) {
    }

    /** Une tuile de départ : nommée ({@code tile}) ou tirée ({@code randomLevel}). */
    record StartingTileDto(Integer depth, String col, String tile, Integer randomLevel) {
    }

    /** Une case de l'océan (profondeur + colonne lettre), pour la base d'opérations. */
    record CellDto(Integer depth, String col) {
    }

    record HexDto(
            Integer row,
            Integer col,
            Integer points,
            List<String> gains,
            Boolean start,
            Boolean offGrid,
            Object capacity,
            String fieldSymbol,
            Integer fieldSymbolCount,
            Integer goal) {
    }

    /**
     * Un objectif de fin de mission, éventuellement à options ({@code chooseOption} et
     * {@code options}, chacune étant un objectif à part entière). Le champ {@code count}
     * nomme un calcul du moteur ; seul {@code field-symbol-sets} est connu, tout autre
     * fait de l'objectif un {@link MissionGoal.Unsupported}. Le {@code majorityBonus} est soit {@code first}/{@code second}, soit
     * un bonus par couleur de symbole de domaine.
     */
    record GoalDto(
            Integer number,
            String id,
            List<String> units,
            List<Integer> depths,
            List<String> columns,
            List<String> zoneContains,
            Boolean zoneDiscoveredByYou,
            Integer pointsPer,
            Map<String, Integer> majorityBonus,
            List<LeaderBonusDto> leaderBonuses,
            String count,
            String columnsFromSeaStar,
            Boolean chooseOption,
            List<GoalDto> options,
            String text) {
    }

    /** Un bonus de leader : par profondeur et/ou colonne, ou (hors de portée) par unité. */
    record LeaderBonusDto(List<Integer> depths, List<String> columns, List<String> units, Integer points) {
    }
}
