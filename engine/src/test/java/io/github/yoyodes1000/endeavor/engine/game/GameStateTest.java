package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import org.junit.jupiter.api.Test;

class GameStateTest {

    @Test
    void unePartieNeuveInstalleLesJoueursEtLeCasier() {
        GameState state = GameState.newGame(3, GameFixtures.roster(), RandomSource.fromSeed(1), 10);

        assertEquals(3, state.playerCount());
        assertEquals(1, state.round());
        for (int i = 0; i < 3; i++) {
            assertEquals(1, state.player(i).specialists().size());
            assertTrue(state.player(i).specialists().get(0).specialist().teamLeader());
            assertEquals(10, state.player(i).reserveDiscs());
        }
        assertEquals(3, state.casier().size()); // les trois tuiles non-chef
        assertTrue(state.casier().stream().noneMatch(specialist -> specialist.teamLeader()));
    }

    @Test
    void refuseUnePartieSansJoueur() {
        assertThrows(IllegalArgumentException.class,
                () -> GameState.newGame(0, GameFixtures.roster(), RandomSource.fromSeed(1), 10));
    }

    @Test
    void leCasierEstEnLectureSeule() {
        GameState state = GameState.newGame(2, GameFixtures.roster(), RandomSource.fromSeed(1), 10);
        assertThrows(UnsupportedOperationException.class, () -> state.casier().add(GameFixtures.teamLeader()));
    }

    @Test
    void laCopieIsoleLesJoueurs() {
        GameState original = GameState.newGame(2, GameFixtures.roster(), RandomSource.fromSeed(1), 10);

        GameState copie = original.copy();
        copie.player(0).moveReserveToTransit(5);

        assertEquals(10, original.player(0).reserveDiscs());
        assertEquals(5, copie.player(0).reserveDiscs());
    }

    @Test
    void laCopieDeLAleaPoursuitLaMemeSuiteSansInfluence() {
        GameState original = GameState.newGame(1, GameFixtures.roster(), RandomSource.fromSeed(42), 10);
        GameState copie = original.copy();

        assertEquals(original.random().nextLong(), copie.random().nextLong());
    }
}
