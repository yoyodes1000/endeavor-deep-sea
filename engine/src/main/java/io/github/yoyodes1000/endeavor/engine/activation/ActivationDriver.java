package io.github.yoyodes1000.endeavor.engine.activation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.game.ActivationCursor;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le driver de la Phase 2 (Activation) : la signature centrale (décision 1) pour
 * cette phase. Chacun à son tour, dans l'ordre du tour, jusqu'à ce que
 * <strong>tous aient passé</strong>.
 *
 * <p>Un tour standard : {@link Activer} un spécialiste (poser un disque de transit
 * sur sa case d'activation libre), puis {@link TerminerTour} pour rendre la main en
 * restant dans la manche. {@link Passer} sort le joueur de la manche définitivement.
 * L'exécution des actions du spécialiste activé, les revues et les jetons — le reste
 * du contenu d'un tour — grandiront sur cette structure (elle est encore
 * <strong>différée</strong>, dépend de l'océan jouable).
 *
 * <p>Sans état : le tour et le round-robin vivent dans l'{@link ActivationCursor},
 * copié avec l'état pour l'IA (déc. 2 et 3).
 */
public final class ActivationDriver {

    private ActivationDriver() {
    }

    /** Entre en Phase 2 : premier joueur du tour, personne n'a encore passé. */
    public static void begin(GameState state) {
        state.setActivationCursor(ActivationCursor.notStarted());
    }

    /** Vrai quand tous les joueurs ont passé : la Phase 2 de la manche est finie. */
    public static boolean isDone(GameState state) {
        return state.activationCursor().passed().size() == state.playerCount();
    }

    /** Les coups légaux du joueur courant (vide si la phase est finie). */
    public static List<Action> legalActions(GameState state) {
        if (isDone(state)) {
            return List.of();
        }
        ActivationCursor cursor = state.activationCursor();
        if (cursor.activatedThisTurn()) {
            // a déjà agi : il termine son tour, ou quitte la manche
            return List.of(new TerminerTour(), new Passer());
        }
        List<Action> actions = new ArrayList<>(activationsOf(state, currentPlayer(state)));
        actions.add(new Passer());
        return actions;
    }

    /**
     * Applique un coup et fait avancer le tour ou le round-robin.
     *
     * @throws IllegalStateException si la phase est finie ou le coup inattendu à ce
     *     stade du tour
     */
    public static void apply(GameState state, Action action) {
        if (isDone(state)) {
            throw new IllegalStateException("La phase d'activation est terminée");
        }
        ActivationCursor cursor = state.activationCursor();
        switch (action) {
            case Activer activer -> {
                if (cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Un seul spécialiste peut être activé par tour");
                }
                state.player(currentPlayer(state)).activate(activer.specialistId());
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(), true));
            }
            case TerminerTour ignored -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Rien à terminer : agir ou passer");
                }
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, cursor.passed()), cursor.passed(), false));
            }
            case Passer ignored -> {
                Set<Integer> passed = new HashSet<>(cursor.passed());
                passed.add(currentPlayer(state));
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, passed), passed, false));
            }
            default -> throw new IllegalStateException("Coup inattendu en activation : " + action);
        }
    }

    /** Les activations légales du joueur : une par spécialiste à case libre, s'il a un disque en transit. */
    private static List<Activer> activationsOf(GameState state, int playerIndex) {
        Player player = state.player(playerIndex);
        if (player.transitDiscs() == 0) {
            return List.of();
        }
        List<Activer> activations = new ArrayList<>();
        for (HeldSpecialist held : player.specialists()) {
            if (held.placedDiscs() == 0) {
                activations.add(new Activer(held.specialist().id()));
            }
        }
        return activations;
    }

    private static int currentPlayer(GameState state) {
        return state.turnOrder().get(state.activationCursor().turnPosition());
    }

    /**
     * Le rang du prochain joueur qui n'a pas passé, en tournant depuis le rang
     * courant. Si tous ont passé, on garde le rang courant (la phase est finie).
     */
    private static int nextActivePosition(GameState state, ActivationCursor cursor, Set<Integer> passed) {
        int count = state.playerCount();
        if (passed.size() >= count) {
            return cursor.turnPosition();
        }
        int position = cursor.turnPosition();
        do {
            position = (position + 1) % count;
        } while (passed.contains(state.turnOrder().get(position)));
        return position;
    }
}
