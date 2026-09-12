package io.github.yoyodes1000.endeavor.app.specialist;

import java.util.List;

/**
 * Reflet brut du fichier {@code specialists.json}, tel que Jackson le lit.
 *
 * <p>Aucune règle ici : les types sont volontairement laxistes (chaînes,
 * nombres emboîtés potentiellement nuls). La traduction vers le modèle du moteur
 * et toute la validation sémantique vivent dans {@link SpecialistLoader} et,
 * au-delà, dans les constructeurs du moteur. Ce DTO n'existe que pour isoler
 * Jackson du moteur — qui, lui, reste sans aucune dépendance.
 */
record SpecialistsDocument(List<Entry> specialists) {

    record Entry(String id, Boolean teamLeader, Integer rank, Side junior, Side senior) {
    }

    record Side(
            String name,
            List<String> immediateGains,
            List<List<String>> actions,
            SpecialAbility specialAbility,
            EndGameScoring endGameScoring) {
    }

    record SpecialAbility(String text) {
    }

    record EndGameScoring(String text, Integer points, Integer per, String count) {
    }
}
