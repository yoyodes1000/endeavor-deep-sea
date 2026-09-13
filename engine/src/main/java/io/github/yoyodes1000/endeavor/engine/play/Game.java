package io.github.yoyodes1000.endeavor.engine.play;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.activation.ActivationDriver;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PreparationCursor;
import io.github.yoyodes1000.endeavor.engine.preparation.PreparationDriver;

import java.util.List;

/**
 * La façade du moteur : le point d'entrée unique qui réalise la signature centrale
 * (décision 1) pour la partie entière, en <strong>dispatchant par phase</strong>
 * vers le driver concerné et en orchestrant les transitions.
 *
 * <p>Une manche enchaîne Préparation puis Activation ; à la fin de l'Activation, on
 * passe à la manche suivante (le premier joueur tourne), ou la partie s'achève après
 * la sixième. Ces transitions sont automatiques : {@link #apply} avance seul jusqu'à
 * la prochaine décision, exactement comme les drivers le font à l'intérieur d'une
 * phase.
 *
 * <p>Ce package est au-dessus des drivers ({@code preparation}, {@code activation}) :
 * il en dépend, aucun ne dépend de lui — pas de cycle.
 */
public final class Game {

    private Game() {
    }

    /** Démarre la partie : ouvre la Phase 1 de la première manche. */
    public static void begin(GameState state) {
        PreparationDriver.begin(state);
        advance(state);
    }

    /** Les coups légaux de la phase courante (vide si la partie est finie). */
    public static List<Action> legalActions(GameState state) {
        return switch (state.phase()) {
            case PREPARATION -> PreparationDriver.legalActions(state);
            case ACTIVATION -> ActivationDriver.legalActions(state);
            case FINISHED -> List.of();
        };
    }

    /**
     * Applique un coup au driver de la phase courante, puis avance seul jusqu'à la
     * prochaine décision (changement de phase ou de manche compris).
     *
     * @throws IllegalStateException si la partie est déjà terminée
     */
    public static void apply(GameState state, Action action) {
        switch (state.phase()) {
            case PREPARATION -> PreparationDriver.apply(state, action);
            case ACTIVATION -> ActivationDriver.apply(state, action);
            case FINISHED -> throw new IllegalStateException("La partie est terminée");
        }
        advance(state);
    }

    /** Enchaîne les transitions de phase et de manche tant qu'aucune décision n'attend. */
    private static void advance(GameState state) {
        boolean progressed = true;
        while (progressed) {
            progressed = switch (state.phase()) {
                case PREPARATION -> enterActivationIfPreparationDone(state);
                case ACTIVATION -> advanceRoundIfActivationDone(state);
                case FINISHED -> false;
            };
        }
    }

    private static boolean enterActivationIfPreparationDone(GameState state) {
        if (!PreparationDriver.isDone(state)) {
            return false;
        }
        ActivationDriver.begin(state);
        state.setPhase(GamePhase.ACTIVATION);
        return true;
    }

    private static boolean advanceRoundIfActivationDone(GameState state) {
        if (!ActivationDriver.isDone(state)) {
            return false;
        }
        if (state.isLastRound()) {
            state.setPhase(GamePhase.FINISHED);
            return true;
        }
        state.enterNextRound();
        state.setCursor(PreparationCursor.notStarted());
        PreparationDriver.begin(state); // fait tourner le premier joueur et ouvre la Phase 1
        state.setPhase(GamePhase.PREPARATION);
        return true;
    }
}
