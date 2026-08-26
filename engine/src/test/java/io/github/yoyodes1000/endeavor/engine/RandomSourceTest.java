package io.github.yoyodes1000.endeavor.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RandomSourceTest {

    private static final int TIRAGES = 1_000;

    @Test
    @DisplayName("Deux sources de même graine produisent la même séquence")
    void memeGrainememeSequence() {
        RandomSource premiere = RandomSource.fromSeed(42L);
        RandomSource seconde = RandomSource.fromSeed(42L);

        for (int i = 0; i < TIRAGES; i++) {
            assertEquals(premiere.nextLong(), seconde.nextLong());
        }
    }

    @Test
    @DisplayName("Deux graines différentes divergent")
    void grainesDifferentesDivergent() {
        assertNotEquals(
                RandomSource.fromSeed(1L).nextLong(),
                RandomSource.fromSeed(2L).nextLong());
    }

    @Test
    @DisplayName("Une copie poursuit la séquence sans influencer l'original")
    void laCopiePoursuitLaSequence() {
        RandomSource original = RandomSource.fromSeed(7L);
        IntStream.range(0, 10).forEach(i -> original.nextLong());

        RandomSource copie = original.copy();

        for (int i = 0; i < TIRAGES; i++) {
            assertEquals(original.nextLong(), copie.nextLong());
        }
    }

    @Test
    @DisplayName("La copie conserve la graine d'origine")
    void laCopieConserveLaGraine() {
        RandomSource original = RandomSource.fromSeed(123L);
        original.nextLong();

        assertEquals(123L, original.copy().seed());
    }

    @Test
    @DisplayName("nextInt reste dans les bornes demandées")
    void nextIntResteDansLesBornes() {
        RandomSource source = RandomSource.fromSeed(99L);

        for (int i = 0; i < TIRAGES; i++) {
            int valeur = source.nextInt(6);
            assertTrue(valeur >= 0 && valeur < 6, "valeur hors bornes : " + valeur);
        }
    }

    @Test
    @DisplayName("nextInt refuse une borne nulle ou négative")
    void nextIntRefuseUneBorneInvalide() {
        RandomSource source = RandomSource.fromSeed(1L);

        assertThrows(IllegalArgumentException.class, () -> source.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> source.nextInt(-3));
    }

    @Test
    @DisplayName("Le mélange est reproductible à graine égale")
    void leMelangeEstReproductible() {
        List<Integer> premiere = nouvelleListe();
        List<Integer> seconde = nouvelleListe();

        RandomSource.fromSeed(2024L).shuffle(premiere);
        RandomSource.fromSeed(2024L).shuffle(seconde);

        assertEquals(premiere, seconde);
    }

    @Test
    @DisplayName("Le mélange conserve tous les éléments")
    void leMelangeConserveLesElements() {
        List<Integer> liste = nouvelleListe();

        RandomSource.fromSeed(5L).shuffle(liste);

        assertEquals(nouvelleListe().size(), liste.size());
        assertTrue(liste.containsAll(nouvelleListe()));
    }

    private static List<Integer> nouvelleListe() {
        return new ArrayList<>(IntStream.range(0, 20).boxed().toList());
    }
}
