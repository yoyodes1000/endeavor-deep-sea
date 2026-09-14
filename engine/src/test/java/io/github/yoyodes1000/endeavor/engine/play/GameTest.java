package io.github.yoyodes1000.endeavor.engine.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.Recuperer;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameTest {

    private static GameState game(int players) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard());
    }

    @Test
    void uneNouvellePartieDemarreEnPreparation() {
        assertEquals(GamePhase.PREPARATION, game(2).phase());
    }

    @Test
    void beginOuvreLeRecrutementDuPremierJoueur() {
        GameState state = game(1);
        Game.begin(state);

        assertEquals(GamePhase.PREPARATION, state.phase());
        assertEquals(List.of(new Recruter("pilot")), Game.legalActions(state));
    }

    @Test
    void laPreparationFinieBasculeEnActivation() {
        GameState state = game(1);
        Game.begin(state);
        Game.apply(state, new Recruter("pilot")); // pas de gains -> 1a/1b/1c enchaînés

        assertEquals(GamePhase.ACTIVATION, state.phase());
        // après l'effort, le joueur a un disque en transit : il peut activer ou passer
        List<Action> coups = Game.legalActions(state);
        assertTrue(coups.contains(new Passer()));
        assertTrue(coups.contains(new Activer("team-leader")));
        assertTrue(coups.contains(new Activer("pilot")));
    }

    @Test
    void unePartieSEnchaineSurSixManchesJusquALaFin() {
        GameState state = game(3);
        Game.begin(state);

        int garde = 0;
        while (state.phase() != GamePhase.FINISHED) {
            List<Action> coups = Game.legalActions(state);
            assertFalse(coups.isEmpty(), "tant que la partie n'est pas finie, un coup est légal");
            Game.apply(state, coups.get(0));
            if (++garde > 1000) {
                fail("la partie ne se termine pas (boucle ?)");
            }
        }

        assertEquals(6, state.round(), "la partie s'achève à la sixième manche");
        assertEquals(GamePhase.FINISHED, state.phase());
        assertTrue(Game.legalActions(state).isEmpty());
    }

    @Test
    void unDisquePoseEnActivationSeRecupereALaMancheSuivante() {
        GameState state = game(1);
        Game.begin(state);
        Game.apply(state, new Recruter("pilot"));  // prépa manche 1 -> activation (transit = 1)
        Game.apply(state, new Activer("team-leader")); // pose un disque sur le team-leader
        Game.apply(state, new Passer());           // fin de l'activation -> manche 2

        assertEquals(2, state.round());
        assertEquals(GamePhase.PREPARATION, state.phase());
        // en 1c de la manche 2, le disque posé au tour précédent est récupérable
        assertEquals(List.of(new Recuperer("team-leader")), Game.legalActions(state));
    }

    @Test
    void appliquerUnCoupApresLaFinEstRefuse() {
        GameState state = game(1);
        Game.begin(state);
        while (state.phase() != GamePhase.FINISHED) {
            Game.apply(state, Game.legalActions(state).get(0));
        }
        assertThrows(IllegalStateException.class, () -> Game.apply(state, new Passer()));
    }
}
