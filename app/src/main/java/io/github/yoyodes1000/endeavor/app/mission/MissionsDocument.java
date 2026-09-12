package io.github.yoyodes1000.endeavor.app.mission;

import java.util.List;

/**
 * Reflet brut de {@code missions.json}, limité à l'identité de la mission et à
 * son plateau Impact. La mise en place, les objectifs et les règles spéciales ne
 * sont pas déclarés : le chargeur les ignore (décompte / Phase 2 à venir).
 */
record MissionsDocument(List<Entry> missions) {

    record Entry(String id, Integer number, String name, ImpactBoardDto impactBoard) {
    }

    record ImpactBoardDto(String orientation, List<HexDto> hexes) {
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
