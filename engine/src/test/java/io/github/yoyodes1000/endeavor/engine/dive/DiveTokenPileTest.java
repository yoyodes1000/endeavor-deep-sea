package io.github.yoyodes1000.endeavor.engine.dive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DiveTokenPileTest {

    private static DiveOption.Gains gains() {
        return new DiveOption.Gains(List.of(Gain.RESEARCH), List.of());
    }

    /** Deux exemplaires de « a », trois de « b » : 5 jetons au total. */
    private static DiveTokenCatalog catalog() {
        return new DiveTokenCatalog(List.of(
                new DiveToken("a", 2, List.of(gains())),
                new DiveToken("b", 3, List.of(gains()))));
    }

    @Test
    void laPiocheDeDepartAUnExemplaireParCopie() {
        DiveTokenPile pile = DiveTokenPile.forGame(catalog());
        assertEquals(5, pile.size());
    }

    @Test
    void tirerRetireLesJetonsDeLaPioche() {
        DiveTokenPile pile = DiveTokenPile.forGame(catalog());

        List<String> drawn = pile.draw(3, RandomSource.fromSeed(1));

        assertEquals(3, drawn.size());
        assertEquals(2, pile.size());
    }

    @Test
    void tirerRendMoinsQueDemandeSiLaPiocheManque() {
        DiveTokenPile pile = DiveTokenPile.forGame(catalog());

        List<String> drawn = pile.draw(10, RandomSource.fromSeed(1));

        assertEquals(5, drawn.size(), "toute la pioche, pas plus");
        assertEquals(0, pile.size());
    }

    @Test
    void respecteLeNombreDeCopiesParType() {
        DiveTokenPile pile = DiveTokenPile.forGame(catalog());
        List<String> drawn = pile.draw(5, RandomSource.fromSeed(7));

        Map<String, Long> counts = drawn.stream().collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        assertEquals(2L, counts.get("a"));
        assertEquals(3L, counts.get("b"));
    }

    @Test
    void laCopieEstIndependante() {
        DiveTokenPile original = DiveTokenPile.forGame(catalog());

        DiveTokenPile copie = original.copy();
        copie.draw(5, RandomSource.fromSeed(1));

        assertEquals(5, original.size(), "la pioche d'origine ne bouge pas");
        assertFalse(copie.size() == original.size(), "la copie a été vidée indépendamment");
    }

    @Test
    void aucuneCopieNeGeleraLaBibliothequeVide() {
        assertTrue(DiveTokenPile.forGame(catalog()).draw(0, RandomSource.fromSeed(1)).isEmpty());
    }
}
