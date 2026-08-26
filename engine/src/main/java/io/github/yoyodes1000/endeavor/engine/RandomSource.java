package io.github.yoyodes1000.endeavor.engine;

import java.util.List;

/**
 * Source d'aléa déterministe et clonable.
 *
 * <p>Le moteur ne doit jamais tirer au hasard implicitement : une partie est
 * entièrement reproductible à partir de sa graine et de son journal d'actions.
 * C'est ce qui rend les tests fiables et les bugs de l'IA rejouables.
 *
 * <p>La recherche Monte-Carlo copie l'état de la partie des milliers de fois
 * par seconde. D'où le choix de SplitMix64 : l'état tient dans un seul entier
 * long, ce qui rend {@link #copy()} pratiquement gratuit — là où
 * {@link java.util.Random} ne se copie pas simplement.
 */
public final class RandomSource {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long MIX_1 = 0xBF58476D1CE4E5B9L;
    private static final long MIX_2 = 0x94D049BB133111EBL;

    private final long seed;
    private long state;

    private RandomSource(long seed, long state) {
        this.seed = seed;
        this.state = state;
    }

    public static RandomSource fromSeed(long seed) {
        return new RandomSource(seed, seed);
    }

    /**
     * Graine d'origine, à enregistrer avec le journal d'actions pour pouvoir
     * rejouer la partie à l'identique.
     */
    public long seed() {
        return seed;
    }

    public long nextLong() {
        state += GOLDEN_GAMMA;
        long z = state;
        z = (z ^ (z >>> 30)) * MIX_1;
        z = (z ^ (z >>> 27)) * MIX_2;
        return z ^ (z >>> 31);
    }

    /**
     * Entier uniforme dans l'intervalle [0, borne[.
     *
     * <p>Le rejet des valeurs hautes évite le biais de modulo : sans lui, les
     * petites valeurs sortiraient légèrement plus souvent.
     *
     * @throws IllegalArgumentException si la borne n'est pas strictement positive
     */
    public int nextInt(int borne) {
        if (borne <= 0) {
            throw new IllegalArgumentException(
                    "La borne doit être strictement positive : " + borne);
        }
        long plafond = (Long.MAX_VALUE / borne) * borne;
        long valeur;
        do {
            valeur = nextLong() >>> 1;
        } while (valeur >= plafond);
        return (int) (valeur % borne);
    }

    /** Mélange la liste sur place, selon Fisher-Yates. */
    public <T> void shuffle(List<T> liste) {
        for (int i = liste.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T memoire = liste.get(i);
            liste.set(i, liste.get(j));
            liste.set(j, memoire);
        }
    }

    /**
     * Copie indépendante, poursuivant la séquence là où celle-ci en est.
     *
     * <p>Deux sources issues d'une même copie produisent la même suite de
     * valeurs sans s'influencer : c'est ce qui permet d'explorer plusieurs
     * branches depuis un même état.
     */
    public RandomSource copy() {
        return new RandomSource(seed, state);
    }
}
