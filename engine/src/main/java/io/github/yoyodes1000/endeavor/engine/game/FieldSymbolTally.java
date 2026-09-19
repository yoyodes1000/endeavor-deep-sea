package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;

import java.util.EnumMap;
import java.util.Map;

/**
 * Les symboles de domaine qu'un joueur a collectionnés : ceux des hexagones du
 * plateau Impact où il a un pion. Une couleur par symbole, plus les jokers qui
 * valent n'importe quelle couleur ; un symbole double compte pour deux.
 */
final class FieldSymbolTally {

    private final Map<FieldSymbol, Integer> byColor = new EnumMap<>(FieldSymbol.class);
    private int wilds;

    private FieldSymbolTally() {
        for (FieldSymbol color : FieldSymbol.values()) {
            byColor.put(color, 0);
        }
    }

    static FieldSymbolTally of(MissionBoard missionBoard, int playerIndex) {
        FieldSymbolTally tally = new FieldSymbolTally();
        for (ImpactHex hex : missionBoard.board().hexes()) {
            if (missionBoard.owner(hex).orElse(-1) == playerIndex) {
                tally.add(hex);
            }
        }
        return tally;
    }

    private void add(ImpactHex hex) {
        if (hex.wild()) {
            wilds += hex.fieldSymbolCount();
        }
        hex.fieldSymbol().ifPresent(color -> byColor.merge(color, hex.fieldSymbolCount(), Integer::sum));
    }

    /**
     * Le nombre de jeux complets des quatre couleurs : les jokers comblent au mieux
     * les couleurs manquantes.
     */
    int completeSets() {
        int sets = 0;
        while (missingForSets(sets + 1) <= wilds) {
            sets++;
        }
        return sets;
    }

    private int missingForSets(int sets) {
        int missing = 0;
        for (int count : byColor.values()) {
            missing += Math.max(0, sets - count);
        }
        return missing;
    }

    /** Le nombre de symboles de la couleur la plus possédée, jokers ajoutés à celle-ci. */
    int mostHeld() {
        int best = byColor.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return best + wilds;
    }
}
