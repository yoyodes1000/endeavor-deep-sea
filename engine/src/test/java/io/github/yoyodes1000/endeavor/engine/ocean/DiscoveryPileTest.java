package io.github.yoyodes1000.endeavor.engine.ocean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscoveryPileTest {

    private static OceanTile tile(String id, int depth, boolean unique) {
        return new OceanTile(id, id, depth, unique, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    /** Catalogue : deux tuiles niveau 1, une niveau 2, une unique, une déjà posée. */
    private static OceanTileCatalog catalog() {
        return new OceanTileCatalog(List.of(
                tile("a", 1, false),
                tile("b", 1, false),
                tile("c", 2, false),
                tile("u", 1, true),
                tile("placed", 1, false)));
    }

    private static OceanBoard boardWithPlaced() {
        OceanBoard board = new OceanBoard(2);
        board.placeTile(new Cell(1, 0), "placed");
        return board;
    }

    @Test
    void laPiocheExclutLesUniquesEtLesTuilesDejaPosees() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        assertEquals(3, pile.size(), "a, b, c ; ni l'unique ni la posée");
        assertEquals(List.of("a", "b"), pile.availableAtLevels(Set.of(1)));
        assertEquals(List.of("c"), pile.availableAtLevels(Set.of(2)));
    }

    @Test
    void tirerRetireLesTuilesDeLaPioche() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        List<String> drawn = pile.draw(2, Set.of(1), RandomSource.fromSeed(1));

        assertEquals(2, drawn.size());
        assertTrue(List.of("a", "b").containsAll(drawn), "tirées parmi les tuiles de niveau 1");
        assertEquals(1, pile.size(), "les deux tirées ont quitté la pioche");
    }

    @Test
    void tirerRendMoinsQueDemandeSiLaPiocheManque() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        List<String> drawn = pile.draw(2, Set.of(2), RandomSource.fromSeed(1));

        assertEquals(List.of("c"), drawn, "une seule tuile de niveau 2 disponible");
    }

    @Test
    void rendreUneTuileLaRemetDansLaPioche() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());
        pile.draw(2, Set.of(1), RandomSource.fromSeed(1)); // retire a et b

        pile.returnTile("a");

        assertEquals(2, pile.size());
        assertTrue(pile.availableAtLevels(Set.of(1)).contains("a"), "a est de nouveau disponible");
    }

    @Test
    void laCopieEstIndependante() {
        DiscoveryPile original = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        DiscoveryPile copie = original.copy();
        copie.draw(3, Set.of(1, 2), RandomSource.fromSeed(1));

        assertEquals(3, original.size(), "la pioche d'origine ne bouge pas");
        assertFalse(copie.size() == original.size(), "la copie a été vidée indépendamment");
    }

    @Test
    void uneTuileCacheeSeGlisseDansLaPiocheDeSonNiveau() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        pile.addTile("u", 1);

        assertEquals(List.of("a", "b", "u"), pile.availableAtLevels(Set.of(1)));
        pile.addTile("u", 1);
        assertEquals(4, pile.size(), "un ajout répété est sans effet");
    }

    @Test
    void uneTuileCacheeSeCopieAvecLaPioche() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());
        DiscoveryPile copy = pile.copy();

        copy.addTile("u", 1);

        assertEquals(3, pile.size(), "la pioche d'origine n'a pas bougé");
        assertEquals(4, copy.size());
    }

    @Test
    void retirerLesNiveauxTropProfondsVideLesPilesConcernees() {
        DiscoveryPile pile = DiscoveryPile.forGame(catalog(), boardWithPlaced());

        pile.removeDeeperThan(1);

        assertEquals(List.of("a", "b"), pile.availableAtLevels(Set.of(1, 2)));
        assertEquals(java.util.Set.of(1), pile.availableDepths());
    }
}
