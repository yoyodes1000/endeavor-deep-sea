package io.github.yoyodes1000.endeavor.engine.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JournalMarketTest {

    private static Journal journal(String id, boolean anchor) {
        return new Journal(id, anchor, id, 1, 0, List.of(FieldSymbol.BLUE), List.of(), List.of());
    }

    /** Exactement 4 revues de départ (marché déterministe : toutes tirées) et 2 standard. */
    private static JournalCatalog catalog() {
        return new JournalCatalog(List.of(
                journal("anchor-1", true), journal("anchor-2", true),
                journal("anchor-3", true), journal("anchor-4", true),
                journal("standard-1", false), journal("standard-2", false)));
    }

    @Test
    void refuseSansCatalogueOuSansAlea() {
        assertThrows(IllegalArgumentException.class, () -> JournalMarket.forGame(null, RandomSource.fromSeed(1)));
        assertThrows(IllegalArgumentException.class, () -> JournalMarket.forGame(catalog(), null));
    }

    @Test
    void refuseMoinsDeQuatreRevuesDeDepart() {
        JournalCatalog tropPeu = new JournalCatalog(List.of(journal("anchor-1", true), journal("standard-1", false)));
        assertThrows(IllegalArgumentException.class, () -> JournalMarket.forGame(tropPeu, RandomSource.fromSeed(1)));
    }

    @Test
    void leMarcheInitialNeContientQueDesRevuesDeDepart() {
        JournalMarket market = JournalMarket.forGame(catalog(), RandomSource.fromSeed(1));

        assertEquals(4, market.underStudy().size());
        assertEquals(Set.of("anchor-1", "anchor-2", "anchor-3", "anchor-4"), Set.copyOf(market.underStudy()));
        assertEquals(2, market.deckSize(), "les 2 revues standard forment la pioche");
    }

    @Test
    void publierRetireDuMarcheEtReassortitDepuisLaPioche() {
        JournalMarket market = JournalMarket.forGame(catalog(), RandomSource.fromSeed(1));
        String published = market.underStudy().get(0);

        market.publish(published, RandomSource.fromSeed(2));

        assertEquals(4, market.underStudy().size(), "réassorti à 4");
        assertTrue(!market.underStudy().contains(published), "la revue publiée a quitté le marché");
        assertEquals(1, market.deckSize(), "une revue tirée de la pioche");
    }

    @Test
    void publierSansPiocheReassortRetrecitLeMarche() {
        JournalCatalog catalog = new JournalCatalog(List.of(
                journal("anchor-1", true), journal("anchor-2", true),
                journal("anchor-3", true), journal("anchor-4", true)));
        JournalMarket market = JournalMarket.forGame(catalog, RandomSource.fromSeed(1));
        String published = market.underStudy().get(0);

        market.publish(published, RandomSource.fromSeed(2));

        assertEquals(3, market.underStudy().size(), "pioche vide : le marché rétrécit");
    }

    @Test
    void refusePublierUneRevueAbsenteDuMarche() {
        JournalMarket market = JournalMarket.forGame(catalog(), RandomSource.fromSeed(1));
        assertThrows(IllegalArgumentException.class,
                () -> market.publish("standard-1", RandomSource.fromSeed(2)));
    }

    @Test
    void laCopieEstIndependante() {
        JournalMarket original = JournalMarket.forGame(catalog(), RandomSource.fromSeed(1));
        JournalMarket copie = original.copy();

        copie.publish(copie.underStudy().get(0), RandomSource.fromSeed(2));

        assertEquals(4, original.underStudy().size(), "l'original n'est pas affecté par la copie");
    }
}
