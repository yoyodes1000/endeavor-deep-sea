package io.github.yoyodes1000.endeavor.engine.journal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * L'ensemble des 32 revues scientifiques du matériel (24 standard + 8 de
 * départ). Le constructeur garantit l'unicité des identifiants — comme pour le
 * reste du modèle, une incohérence fait échouer le chargement.
 */
public record JournalCatalog(List<Journal> journals) {

    public JournalCatalog {
        if (journals == null || journals.isEmpty()) {
            throw new IllegalArgumentException("Le catalogue de revues est vide");
        }
        journals = List.copyOf(journals);

        Map<String, Journal> byId = new LinkedHashMap<>();
        for (Journal journal : journals) {
            if (byId.put(journal.id(), journal) != null) {
                throw new IllegalArgumentException("Identifiant de revue en double : " + journal.id());
            }
        }
    }

    /** La revue portant cet identifiant, si le catalogue en connaît une. */
    public Optional<Journal> byId(String id) {
        return journals.stream().filter(journal -> journal.id().equals(id)).findFirst();
    }
}
