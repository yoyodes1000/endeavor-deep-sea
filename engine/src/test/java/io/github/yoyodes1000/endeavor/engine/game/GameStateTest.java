package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import org.junit.jupiter.api.Test;

class GameStateTest {

    @Test
    void unePartieNeuveInstalleLesJoueursEtLeCasier() {
        GameState state = GameFixtures.newGame(3, 1);

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
        assertThrows(IllegalArgumentException.class, () -> GameFixtures.newGame(0, 1));
    }

    @Test
    void refuseUnePartieSansPlateauDeMission() {
        assertThrows(IllegalArgumentException.class,
                () -> GameState.newGame(2, GameFixtures.roster(), RandomSource.fromSeed(1), 10, null));
    }

    @Test
    void leCasierEstEnLectureSeule() {
        GameState state = GameFixtures.newGame(2, 1);
        assertThrows(UnsupportedOperationException.class, () -> state.casier().add(GameFixtures.teamLeader()));
    }

    @Test
    void laCopieIsoleLesJoueurs() {
        GameState original = GameFixtures.newGame(2, 1);

        GameState copie = original.copy();
        copie.player(0).moveReserveToTransit(5);

        assertEquals(10, original.player(0).reserveDiscs());
        assertEquals(5, copie.player(0).reserveDiscs());
    }

    @Test
    void laCopieIsoleLePlateauDeMission() {
        GameState original = GameFixtures.newGame(2, 1);
        ImpactHex depart = original.missionBoard().board().hexAt(0, 0).orElseThrow();

        GameState copie = original.copy();
        copie.missionBoard().place(depart, 0);

        assertFalse(original.missionBoard().isOccupied(depart), "l'occupation de l'original ne bouge pas");
        assertTrue(copie.missionBoard().isOccupied(depart));
    }

    @Test
    void laCopieDeLAleaPoursuitLaMemeSuiteSansInfluence() {
        GameState original = GameFixtures.newGame(1, 42);
        GameState copie = original.copy();

        assertEquals(original.random().nextLong(), copie.random().nextLong());
    }

    @Test
    void lOrdreDuTourPartDuPremierJoueurEnTournant() {
        GameState state = GameFixtures.newGame(4, 1);
        state.setFirstPlayerIndex(2);
        assertEquals(java.util.List.of(2, 3, 0, 1), state.turnOrder());
    }

    @Test
    void laMancheSuivanteIncremente() {
        GameState state = GameFixtures.newGame(2, 1);
        assertEquals(1, state.round());
        state.enterNextRound();
        assertEquals(2, state.round());
    }

    @Test
    void refuseUnPremierJoueurHorsBornes() {
        GameState state = GameFixtures.newGame(2, 1);
        assertThrows(IllegalArgumentException.class, () -> state.setFirstPlayerIndex(2));
    }
}
