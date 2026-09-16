package io.github.yoyodes1000.endeavor.engine.dive;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;

import java.util.List;

/**
 * Empile les jetons de plongée des sites d'une zone fraîchement en jeu — mise en
 * place (les tuiles de départ) et découverte (une tuile nouvellement posée)
 * partagent le même geste. Fonction pure côté effet de bord unique (le board et
 * la pioche), pas d'état propre.
 */
public final class DiveSiteSetup {

    private DiveSiteSetup() {
    }

    /** Tire et empile les jetons de chaque site de plongée de {@code tile} sur {@code cell}. */
    public static void stack(OceanBoard board, DiveTokenPile pile, RandomSource random, Cell cell, OceanTile tile) {
        for (DiveSite site : tile.diveSites()) {
            List<String> drawn = pile.draw(site.tokenCount(), random);
            board.stackDiveTokens(cell, site.id(), drawn);
        }
    }
}
