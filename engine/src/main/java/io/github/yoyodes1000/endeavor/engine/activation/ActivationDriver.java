package io.github.yoyodes1000.endeavor.engine.activation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.game.ActivationCursor;
import io.github.yoyodes1000.endeavor.engine.game.GameState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le driver de la Phase 2 (Activation) : la signature centrale (décision 1) pour
 * cette phase. Chacun à son tour, dans l'ordre du tour, jusqu'à ce que
 * <strong>tous aient passé</strong>.
 *
 * <p>Version <strong>minimale</strong> : le seul coup est {@link Passer} (sortir de
 * la manche). L'activation d'un spécialiste, l'exécution des actions, les revues et
 * les jetons — le vrai contenu d'un tour — s'ajouteront ici, sur la même structure.
 *
 * <p>Comme le driver de préparation, il est sans état : le round-robin vit dans
 * l'état ({@link ActivationCursor}), copié avec lui pour l'IA (déc. 2 et 3).
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
        return List.of(new Passer());
    }

    /**
     * Applique un coup et avance le round-robin jusqu'au prochain joueur encore en
     * lice, ou termine la phase.
     *
     * @throws IllegalStateException si la phase est finie ou le coup inattendu
     */
    public static void apply(GameState state, Action action) {
        if (isDone(state)) {
            throw new IllegalStateException("La phase d'activation est terminée");
        }
        if (!(action instanceof Passer)) {
            throw new IllegalStateException("Coup inattendu en activation : " + action);
        }
        ActivationCursor cursor = state.activationCursor();
        int current = state.turnOrder().get(cursor.turnPosition());

        Set<Integer> passed = new HashSet<>(cursor.passed());
        passed.add(current);
        state.setActivationCursor(new ActivationCursor(nextActivePosition(state, cursor, passed), passed));
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
