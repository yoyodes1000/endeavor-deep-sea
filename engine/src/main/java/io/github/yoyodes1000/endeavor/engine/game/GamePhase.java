package io.github.yoyodes1000.endeavor.engine.game;

/**
 * La phase courante d'une manche, du point de vue de la façade {@code Game}.
 *
 * <p>Une manche enchaîne {@link #PREPARATION} puis {@link #ACTIVATION} ; après la
 * sixième manche, la partie est {@link #FINISHED}. La façade s'en sert pour
 * dispatcher les coups vers le bon driver et pour orchestrer les transitions.
 */
public enum GamePhase {
    PREPARATION,
    ACTIVATION,
    FINISHED
}
