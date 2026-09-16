package io.github.yoyodes1000.endeavor.app.journal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.journal.Journal;
import io.github.yoyodes1000.endeavor.engine.journal.JournalCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Charge le catalogue des revues scientifiques depuis le JSON du matériel et le
 * traduit vers le modèle du moteur. Même partage que les autres chargeurs :
 * Jackson lit la forme, le moteur valide le vocabulaire ({@code fromCode}) et la
 * sémantique (constructeurs). Chargeur agnostique de l'I/O.
 *
 * <p>Le champ {@code actions} des données n'est pas déclaré dans le reflet JSON :
 * le mapper est réglé pour <strong>ignorer</strong> les propriétés inconnues, le
 * temps que cette mécanique soit conçue sur des cas réels.
 */
public final class JournalLoader {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Lit et valide le catalogue.
     *
     * @throws IllegalArgumentException si une donnée est incohérente (vocabulaire
     *     inconnu, champ requis manquant, identifiant en double…)
     * @throws UncheckedIOException si le flux est illisible ou mal formé
     */
    public JournalCatalog load(Reader source) {
        JournalsDocument document = read(source);
        if (document == null || document.journals() == null) {
            throw new IllegalArgumentException("JSON des revues vide ou sans tableau « journals »");
        }
        List<Journal> journals = document.journals().stream()
                .map(JournalLoader::toJournal)
                .toList();
        return new JournalCatalog(journals);
    }

    private JournalsDocument read(Reader source) {
        try {
            return mapper.readValue(source, JournalsDocument.class);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON des revues illisible", e);
        }
    }

    private static Journal toJournal(JournalsDocument.Entry entry) {
        if (entry.researchCost() == null) {
            throw new IllegalArgumentException("Coût de recherche manquant pour la revue " + entry.id());
        }
        if (entry.victoryPoints() == null) {
            throw new IllegalArgumentException("Points de victoire manquants pour la revue " + entry.id());
        }
        return new Journal(
                entry.id(),
                Boolean.TRUE.equals(entry.anchor()),
                entry.name(),
                entry.researchCost(),
                entry.victoryPoints(),
                fieldSymbols(entry.fieldSymbols()),
                gains(entry.publisherGains()),
                gains(entry.opponentsGains()));
    }

    private static List<FieldSymbol> fieldSymbols(List<String> raw) {
        return raw == null ? List.of() : raw.stream().map(FieldSymbol::fromCode).toList();
    }

    private static List<Gain> gains(List<String> raw) {
        return raw == null ? List.of() : raw.stream().map(Gain::fromCode).toList();
    }
}
