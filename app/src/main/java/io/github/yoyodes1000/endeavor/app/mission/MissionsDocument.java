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
            List<ShuffledRowDto> shuffledRows,
            List<HiddenTileDto> hiddenTiles,
            Integer maxDepth,
            CellDto baseOfOperations,
            Integer startingVessels) {
    }

    /** Une ligne dont les tuiles sont mélangées entre ses colonnes (lettres). */
    record ShuffledRowDto(Integer depth, List<String> columns, List<RowTileDto> tiles) {
    }

    /** Une tuile d'une ligne mélangée : nommée ({@code tile}) ou tirée ({@code randomLevel}). */
    record RowTileDto(String tile, Integer randomLevel) {
    }

    /** Une tuile glissée dans la pioche de son niveau (les autres marqueurs du relevé sont tolérés). */
    record HiddenTileDto(String tile, Integer level) {
    }

    /** Une tuile de départ : nommée ({@code tile}) ou tirée ({@code randomLevel}). */
    record StartingTileDto(Integer depth, String col, String tile, Integer randomLevel) {
    }

    /**
     * La base d'opérations : une case de l'océan (profondeur + colonne lettre) ou, quand la
     * case n'est pas connue d'avance, la tuile qui fait office de base ({@code tile}).
     */
    record CellDto(Integer depth, String col, String tile) {
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
