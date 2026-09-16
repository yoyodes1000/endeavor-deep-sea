package io.github.yoyodes1000.endeavor.engine.dive;

import io.github.yoyodes1000.endeavor.engine.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * La pioche partagée des 36 jetons de plongée physiques, encore à empiler sur des
 * sites. Contrairement aux pistes Sonar (une pioche par tuile), c'est un
 * <strong>sac unique</strong> : chaque site nouvellement en jeu (mise en place ou
 * découverte) y tire ses jetons au hasard, à mesure que des zones entrent en jeu.
 *
 * <p>Pas de regroupement par niveau (contrairement à {@link
 * io.github.yoyodes1000.endeavor.engine.ocean.DiscoveryPile}) : les 36 exemplaires
 * sont interchangeables au tirage, seul leur effet propre les distingue — révélé
 * seulement quand un jeton est pris. Pièce d'état mutable, copiée avec la partie
 * (déc. 3) pour qu'une même graine rejoue les mêmes tirages.
 */
public final class DiveTokenPile {

    private final List<String> available;

    private DiveTokenPile(List<String> available) {
        this.available = new ArrayList<>(available);
    }

    /** La pioche de départ : un exemplaire par copie de chaque type du catalogue. */
    public static DiveTokenPile forGame(DiveTokenCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Le catalogue de jetons de plongée est requis");
        }
        List<String> available = new ArrayList<>();
        for (DiveToken token : catalog.tokens()) {
            for (int i = 0; i < token.copies(); i++) {
                available.add(token.id());
            }
        }
        return new DiveTokenPile(available);
    }

    /**
     * Tire {@code count} jetons au hasard, les retire de la pioche et les renvoie
     * (l'ordre du tirage fixe l'ordre d'empilement, index 0 = sommet). En renvoie
     * moins si la pioche n'en a pas assez.
     *
     * @throws IllegalArgumentException si {@code count} est négatif ou la source nulle
     */
    public List<String> draw(int count, RandomSource random) {
        if (count < 0) {
            throw new IllegalArgumentException("Nombre de jetons à tirer négatif : " + count);
        }
        if (random == null) {
            throw new IllegalArgumentException("La source d'aléa est requise");
        }
        List<String> drawn = new ArrayList<>();
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            drawn.add(available.remove(random.nextInt(available.size())));
        }
        return drawn;
    }

    /** Combien de jetons restent à tirer. */
    public int size() {
        return available.size();
    }

    /** Copie indépendante, pour isoler une simulation (décision 3). */
    public DiveTokenPile copy() {
        return new DiveTokenPile(available);
    }
}
