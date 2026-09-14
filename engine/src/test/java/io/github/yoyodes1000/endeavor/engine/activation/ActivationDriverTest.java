package io.github.yoyodes1000.endeavor.engine.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivationDriverTest {

    private static GameState game(int players) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard());
    }

    @Test
    void auDebutSeulPasserEstLegalEtLaPhaseNEstPasFinie() {
        GameState state = game(3);
        ActivationDriver.begin(state);

        assertFalse(ActivationDriver.isDone(state));
        assertEquals(List.of(new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void laPhaseFinitQuandTousLesJoueursOntPasse() {
        GameState state = game(3);
        ActivationDriver.begin(state);

        ActivationDriver.apply(state, new Passer());
        assertFalse(ActivationDriver.isDone(state));
        ActivationDriver.apply(state, new Passer());
        assertFalse(ActivationDriver.isDone(state));
        ActivationDriver.apply(state, new Passer());

        assertTrue(ActivationDriver.isDone(state));
        assertTrue(ActivationDriver.legalActions(state).isEmpty());
    }

    @Test
    void leRoundRobinSuitLOrdreDuTour() {
        GameState state = game(3);
        state.setFirstPlayerIndex(2); // ordre du tour : [2, 0, 1]
        ActivationDriver.begin(state);

        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2), state.activationCursor().passed(), "le premier joueur passe d'abord");
        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2, 0), state.activationCursor().passed());
        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2, 0, 1), state.activationCursor().passed());
    }

    @Test
    void passerApresLaFinDePhaseEstRefuse() {
        GameState state = game(1);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Passer());

        assertTrue(ActivationDriver.isDone(state));
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Passer()));
    }

    @Test
    void unCoupInattenduEnActivationEstRefuse() {
        GameState state = game(2);
        ActivationDriver.begin(state);
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Recruter("pilot")));
    }

    @Test
    void activerConsommeUnDisquePuisSeulTerminerOuPasserRestent() {
        GameState state = game(1);
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);

        assertTrue(ActivationDriver.legalActions(state).contains(new Activer("team-leader")));
        ActivationDriver.apply(state, new Activer("team-leader"));

        assertEquals(1, state.player(0).transitDiscs(), "un disque consommé");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void unSeulSpecialistePeutEtreActiveParTour() {
        GameState state = game(1);
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("team-leader"));

        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Activer("team-leader")));
    }

    @Test
    void terminerTourRendLaMainSansSortirDeLaManche() {
        GameState state = game(2);
        state.player(0).moveReserveToTransit(1);
        ActivationDriver.begin(state); // ordre du tour [0, 1], joueur courant 0
        ActivationDriver.apply(state, new Activer("team-leader"));
        ActivationDriver.apply(state, new TerminerTour());

        assertEquals(1, state.activationCursor().turnPosition(), "au joueur suivant");
        assertTrue(state.activationCursor().passed().isEmpty(), "personne n'a quitté la manche");
        assertFalse(ActivationDriver.isDone(state));
    }

    @Test
    void terminerTourSansAvoirAgiEstRefuse() {
        GameState state = game(1);
        ActivationDriver.begin(state);
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new TerminerTour()));
    }
}
