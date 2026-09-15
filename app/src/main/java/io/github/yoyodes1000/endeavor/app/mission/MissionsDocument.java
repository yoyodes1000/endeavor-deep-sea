package io.github.yoyodes1000.endeavor.app.mission;

import java.util.List;

/**
 * Reflet brut de {@code missions.json}, limité à l'identité de la mission, à son
 * plateau Impact et à la mise en place de l'océan ({@code setup} : colonnes et
 * tuiles de départ). Les objectifs et les règles spéciales ne sont pas déclarés :
 * le chargeur les ignore (décompte / Phase 2 à venir).
 */
record MissionsDocument(List<Entry> missions) {

    record Entry(String id, Integer number, String name, ImpactBoardDto impactBoard, SetupDto setup) {
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
            Object capacity) {
    }
}
