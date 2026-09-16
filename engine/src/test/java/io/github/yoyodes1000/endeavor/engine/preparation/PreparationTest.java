package io.github.yoyodes1000.endeavor.engine.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import org.junit.jupiter.api.Test;

class PreparationTest {

    private static GameState game(int players, long seed) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(seed), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());
    }

    @Test
    void leTirageDuPremierJoueurEstReproductible() {
        GameState a = game(3, 12345);
        GameState b = game(3, 12345);

        Preparation.chooseFirstPlayer(a);
        Preparation.chooseFirstPlayer(b);

        assertEquals(a.firstPlayerIndex(), b.firstPlayerIndex(), "même graine → même premier joueur");
        assertTrue(a.firstPlayerIndex() >= 0 && a.firstPlayerIndex() < 3);
    }

    @Test
    void lePremierJoueurTourneApresLaPremiereManche() {
        GameState state = game(4, 1);
        Preparation.chooseFirstPlayer(state); // manche 1 : tirage
        int attendu = state.firstPlayerIndex();

        for (int manche = 2; manche <= 6; manche++) {
            state.enterNextRound();
            Preparation.chooseFirstPlayer(state);
            attendu = (attendu + 1) % 4;
            assertEquals(attendu, state.firstPlayerIndex(), "manche " + manche);
        }
    }

    @Test
    void lEffortPrendAutantDeDisquesQueLeNiveauDInspiration() {
        Player player = game(1, 1).player(0);

        // au départ, inspiration au niveau 1 -> 1 disque déplacé
        Preparation.applyEffort(player);
        assertEquals(1, player.transitDiscs());
        assertEquals(9, player.reserveDiscs());

        // inspiration portée au niveau 3 (case 4) -> 3 disques
        player.attributes().advance(Attribute.INSPIRATION, 4);
        Preparation.applyEffort(player);
        assertEquals(4, player.transitDiscs());
        assertEquals(6, player.reserveDiscs());
    }
}
