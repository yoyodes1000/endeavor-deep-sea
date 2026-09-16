package io.github.yoyodes1000.endeavor.app.journal;

import java.util.List;

/**
 * Reflet brut de {@code journals.json} : chaque revue, son coût, ses points,
 * ses symboles de domaine et ses gains. Le champ {@code actions} (disque
 * d'activation après acquisition, mécanique à part) n'est pas déclaré ;
 * Jackson l'ignore.
 */
record JournalsDocument(List<Entry> journals) {

    record Entry(
            String id,
            Boolean anchor,
            String name,
            Integer researchCost,
            Integer victoryPoints,
            List<String> fieldSymbols,
            List<String> publisherGains,
            List<String> opponentsGains) {
    }
}
