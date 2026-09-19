package io.github.yoyodes1000.endeavor.app.mission;

import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;

import java.util.List;

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
            Integer fieldSymbolCount) {
    }

    /**
     * Un objectif de fin de mission. Les champs au-delà de la forme standard
     * ({@code leaderBonuses}, {@code count}, {@code columnsFromSeaStar},
     * {@code chooseOption}, {@code zoneDiscoveredByYou}, un {@code majorityBonus}
     * par couleur) servent seulement au chargeur à reconnaître un objectif hors
     * de portée ({@link MissionGoal.Unsupported}) ; leur contenu n'est pas
     * interprété.
     */
    record GoalDto(
            Integer number,
            List<String> units,
            List<Integer> depths,
            List<String> columns,
            List<String> zoneContains,
            Boolean zoneDiscoveredByYou,
            Integer pointsPer,
            MajorityBonusDto majorityBonus,
            Object leaderBonuses,
            String count,
            String columnsFromSeaStar,
            Boolean chooseOption,
            String text) {
    }

    record MajorityBonusDto(Integer first, Integer second) {
    }
}
