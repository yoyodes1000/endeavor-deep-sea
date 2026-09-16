package io.github.yoyodes1000.endeavor.app.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.journal.Journal;
import io.github.yoyodes1000.endeavor.engine.journal.JournalCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalLoaderTest {

    private static final Path CATALOG = Path.of("..", "data", "journals.json");

    private final JournalLoader loader = new JournalLoader();

    private JournalCatalog realCatalog() throws Exception {
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            return loader.load(reader);
        }
    }

    @Test
    void chargeLesTrenteDeuxRevuesReelles() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());

        JournalCatalog catalog = realCatalog();

        assertEquals(32, catalog.journals().size());
        assertEquals(8, catalog.journals().stream().filter(Journal::anchor).count(), "8 revues de départ");
        assertEquals(24, catalog.journals().stream().filter(j -> !j.anchor()).count(), "24 revues standard");
    }

    @Test
    void chargeLesChampsDUneRevueStandard() throws Exception {
        JournalCatalog catalog = realCatalog();

        Journal journal = catalog.byId("advanced-pressure-venting-technology").orElseThrow();
        assertEquals("Advanced Pressure-Venting Technology", journal.name());
        assertEquals(2, journal.researchCost());
        assertEquals(2, journal.victoryPoints());
        assertEquals(List.of(FieldSymbol.BLUE, FieldSymbol.YELLOW), journal.fieldSymbols());
        assertEquals(List.of(Gain.INGENUITY, Gain.INGENUITY), journal.publisherGains());
        assertTrue(journal.opponentsGains().isEmpty());
        assertFalse(journal.anchor());
    }

    @Test
    void chargeUneRevueDeDepartAvecGainsPourLesAdversaires() throws Exception {
        JournalCatalog catalog = realCatalog();

        Journal journal = catalog.byId("effective-communication-of-ecosystemic-risk").orElseThrow();
        assertTrue(journal.anchor());
        assertEquals(List.of(Gain.DISC), journal.opponentsGains());
    }

    @Test
    void ignoreLeChampActionsNonModelise() throws Exception {
        JournalCatalog catalog = realCatalog();

        // « the-nautical-journal » porte deux emplacements d'activation (actions), ignorés au chargement
        Journal journal = catalog.byId("the-nautical-journal").orElseThrow();
        assertTrue(journal.publisherGains().isEmpty());
    }

    @Test
    void refuseUnCoutDeRechercheManquant() {
        String json = "{\"journals\":[{\"id\":\"j\",\"anchor\":false,\"name\":\"J\",\"victoryPoints\":0,"
                + "\"fieldSymbols\":[\"blue\"],\"publisherGains\":[],\"opponentsGains\":[]}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnSymboleDeDomaineInconnu() {
        String json = "{\"journals\":[{\"id\":\"j\",\"anchor\":false,\"name\":\"J\",\"researchCost\":1,"
                + "\"victoryPoints\":0,\"fieldSymbols\":[\"purple\"],\"publisherGains\":[],\"opponentsGains\":[]}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        String json = "{\"journals\":["
                + journal("dup") + "," + journal("dup") + "]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    private static String journal(String id) {
        return "{\"id\":\"" + id + "\",\"anchor\":false,\"name\":\"" + id + "\",\"researchCost\":1,"
                + "\"victoryPoints\":0,\"fieldSymbols\":[\"blue\"],\"publisherGains\":[],\"opponentsGains\":[]}";
    }
}
