package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerViewTest {

    @Test
    void laVueSepareLeJoueurDeSesAdversaires() {
        GameState state = GameFixtures.newGame(3, 1);
        PlayerView view = state.viewFor(1);

        assertEquals(1, view.viewerIndex());
        assertSame(state.player(1), view.self());
        assertEquals(2, view.opponents().size());
        assertFalse(view.opponents().contains(state.player(1)), "on ne se voit pas comme adversaire");
        assertTrue(view.opponents().contains(state.player(0)));
        assertTrue(view.opponents().contains(state.player(2)));
        assertEquals(3, view.casier().size());
        assertEquals(1, view.round());
    }

    @Test
    void refuseUnIndiceHorsBornes() {
        GameState state = GameFixtures.newGame(2, 1);
        assertThrows(IllegalArgumentException.class, () -> state.viewFor(2));
        assertThrows(IllegalArgumentException.class, () -> state.viewFor(-1));
    }
}
