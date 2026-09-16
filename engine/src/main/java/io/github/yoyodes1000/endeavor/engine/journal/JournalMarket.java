package io.github.yoyodes1000.endeavor.engine.journal;

import io.github.yoyodes1000.endeavor.engine.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le marché des revues « à l'étude » : à la mise en place, on tire 4 des 8
 * revues de départ (anchor) au hasard pour former le marché initial, et on
 * <strong>écarte définitivement</strong> les 4 autres — ni en jeu, ni en
 * pioche. La pioche de réassort ne contient que les 24 revues standard.
 *
 * <p>À chaque publication, la revue acquise quitte le marché et, si la pioche
 * n'est pas épuisée, une nouvelle la remplace — un marché à moins de 4 revues
 * est donc possible en fin de partie. Pièce d'état mutable, copiée avec la
 * partie (décision 3).
 */
public final class JournalMarket {

    private static final int MARKET_SIZE = 4;

    private final List<String> underStudy;
    private final List<String> deck;

    private JournalMarket(List<String> underStudy, List<String> deck) {
        this.underStudy = new ArrayList<>(underStudy);
        this.deck = new ArrayList<>(deck);
    }

    /**
     * Met en place le marché : tire 4 revues de départ au hasard, écarte les
     * autres, et prépare la pioche des revues standard.
     *
     * @throws IllegalArgumentException si le catalogue ou la source d'aléa sont nuls,
     *     ou si le catalogue compte moins de 4 revues de départ
     */
    public static JournalMarket forGame(JournalCatalog catalog, RandomSource random) {
        if (catalog == null || random == null) {
            throw new IllegalArgumentException("Le catalogue de revues et la source d'aléa sont requis");
        }
        List<String> anchorPool = new ArrayList<>();
        List<String> deck = new ArrayList<>();
        for (Journal journal : catalog.journals()) {
            (journal.anchor() ? anchorPool : deck).add(journal.id());
        }
        if (anchorPool.size() < MARKET_SIZE) {
            throw new IllegalArgumentException(
                    "Il faut au moins " + MARKET_SIZE + " revues de départ, le catalogue en a "
                            + anchorPool.size());
        }
        List<String> underStudy = new ArrayList<>();
        for (int i = 0; i < MARKET_SIZE; i++) {
            underStudy.add(anchorPool.remove(random.nextInt(anchorPool.size())));
        }
        return new JournalMarket(underStudy, deck);
    }

    /** Les revues actuellement à l'étude (au plus {@value #MARKET_SIZE}, moins si la pioche est épuisée). */
    public List<String> underStudy() {
        return Collections.unmodifiableList(underStudy);
    }

    /** Combien de revues restent dans la pioche de réassort. */
    public int deckSize() {
        return deck.size();
    }

    /**
     * Retire une revue du marché (elle vient d'être publiée) et réassortit depuis
     * la pioche si elle n'est pas vide.
     *
     * @throws IllegalArgumentException si la revue n'est pas à l'étude
     */
    public void publish(String journalId, RandomSource random) {
        if (!underStudy.remove(journalId)) {
            throw new IllegalArgumentException("Revue absente du marché à l'étude : " + journalId);
        }
        if (!deck.isEmpty()) {
            underStudy.add(deck.remove(random.nextInt(deck.size())));
        }
    }

    /** Copie indépendante, pour isoler une simulation (décision 3). */
    public JournalMarket copy() {
        return new JournalMarket(underStudy, deck);
    }
}
