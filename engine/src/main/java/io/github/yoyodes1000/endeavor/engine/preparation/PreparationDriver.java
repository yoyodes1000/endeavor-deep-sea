package io.github.yoyodes1000.endeavor.engine.preparation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.Recuperer;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.ImpactPlacement;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PreparationCursor;
import io.github.yoyodes1000.endeavor.engine.game.PreparationCursor.Step;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;

import java.util.ArrayList;
import java.util.List;

/**
 * Le driver de la Phase 1 (Préparation) : la réalisation de la signature centrale
 * (décision 1) pour cette phase. Il enchaîne, dans l'ordre du tour et pour chaque
 * joueur, 1a recrutement → cascade de pose des impacts → 1b effort → 1c
 * récupération, en <strong>avançant seul jusqu'au prochain point de décision</strong>.
 *
 * <p>Le « où en est-on » vit dans l'état ({@link PreparationCursor}), pas dans ce
 * driver : les méthodes sont statiques, sans état, et l'état copié emporte tout ce
 * qu'il faut pour reprendre une simulation en cours (déc. 2 et 3).
 *
 * <p>Deux étapes de la phase sont automatiques (premier joueur, effort) : elles ne
 * sont pas des coups, {@link #apply} les joue au passage. Les submersibles gagnés
 * sont comptés dans le curseur mais leur arrivée n'est <strong>pas encore</strong>
 * résolue (elle dépend de la mise en place de la mission) : différé assumé.
 */
public final class PreparationDriver {

    private PreparationDriver() {
    }

    /**
     * Entre dans la Phase 1 de la manche courante : fixe le premier joueur (étape
     * automatique) puis avance jusqu'au premier point de décision.
     *
     * @throws IllegalStateException si la phase est déjà entamée
     */
    public static void begin(GameState state) {
        if (state.cursor().step() != Step.NOT_STARTED) {
            throw new IllegalStateException("La phase de préparation est déjà entamée");
        }
        Preparation.chooseFirstPlayer(state);
        state.setCursor(new PreparationCursor(0, Step.RECRUIT, 0, 0, 0));
        settle(state);
    }

    /** Vrai quand la Phase 1 de la manche est terminée pour tous les joueurs. */
    public static boolean isDone(GameState state) {
        return state.cursor().step() == Step.DONE;
    }

    /** Les coups légaux au point de décision courant (vide si la phase est finie). */
    public static List<Action> legalActions(GameState state) {
        PreparationCursor cursor = state.cursor();
        return switch (cursor.step()) {
            case RECRUIT -> new ArrayList<>(Recruitment.legalRecruits(state, currentPlayer(state)));
            case PLACE_IMPACT -> state.missionBoard().legalPlacements().stream()
                    .map(hex -> (Action) new PoserImpact(hex.row(), hex.col()))
                    .toList();
            case RECOVER -> state.player(currentPlayer(state)).specialists().stream()
                    .filter(held -> held.placedDiscs() > 0)
                    .map(held -> (Action) new Recuperer(held.specialist().id()))
                    .toList();
            case NOT_STARTED, DONE -> List.of();
        };
    }

    /**
     * Applique un coup puis avance seul jusqu'au prochain point de décision.
     *
     * @throws IllegalStateException si le coup ne correspond pas à l'étape courante
     */
    public static void apply(GameState state, Action action) {
        PreparationCursor cursor = state.cursor();
        switch (cursor.step()) {
            case RECRUIT -> {
                Recruter recruit = require(action, Recruter.class, cursor.step());
                EffectOutcome outcome = Recruitment.applyRecruit(state, currentPlayer(state), recruit);
                state.setCursor(new PreparationCursor(cursor.turnPosition(), Step.PLACE_IMPACT,
                        outcome.impactsEarned(), cursor.pendingVessels() + outcome.vesselsEarned(), 0));
            }
            case PLACE_IMPACT -> {
                PoserImpact placement = require(action, PoserImpact.class, cursor.step());
                int player = currentPlayer(state);
                ImpactHex hex = state.missionBoard().board().hexAt(placement.row(), placement.col())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Aucun hexagone en " + placement.row() + "," + placement.col()));
                EffectOutcome outcome = ImpactPlacement.place(state.missionBoard(), state.player(player), hex, player);
                int stillPending = cursor.pendingImpacts() - 1 + outcome.impactsEarned();
                state.setCursor(new PreparationCursor(cursor.turnPosition(), Step.PLACE_IMPACT,
                        stillPending, cursor.pendingVessels() + outcome.vesselsEarned(), 0));
            }
            case RECOVER -> {
                Recuperer recovery = require(action, Recuperer.class, cursor.step());
                state.player(currentPlayer(state)).recoverDisc(recovery.specialistId());
                state.setCursor(new PreparationCursor(cursor.turnPosition(), Step.RECOVER,
                        0, cursor.pendingVessels(), cursor.remainingRecoveries() - 1));
            }
            case NOT_STARTED, DONE -> throw new IllegalStateException(
                    "Aucun coup attendu à l'étape " + cursor.step());
        }
        settle(state);
    }

    /**
     * Avance à travers les étapes automatiques et les décisions sans choix (rien à
     * recruter, cascade finie, plus rien à récupérer) jusqu'à s'arrêter sur un vrai
     * point de décision ou sur la fin de phase.
     */
    private static void settle(GameState state) {
        while (true) {
            PreparationCursor cursor = state.cursor();
            switch (cursor.step()) {
                case RECRUIT -> {
                    if (!Recruitment.legalRecruits(state, currentPlayer(state)).isEmpty()) {
                        return; // vrai choix : on s'arrête
                    }
                    state.setCursor(effortThenRecover(state, cursor)); // rien à recruter, pas de cascade
                }
                case PLACE_IMPACT -> {
                    if (cursor.pendingImpacts() > 0 && !state.missionBoard().legalPlacements().isEmpty()) {
                        return; // il reste un impact à poser, et de la place
                    }
                    state.setCursor(effortThenRecover(state, cursor)); // cascade finie (ou plus de place)
                }
                case RECOVER -> {
                    if (cursor.remainingRecoveries() > 0 && state.player(currentPlayer(state)).hasRecoverableDisc()) {
                        return; // vrai choix : sur quel spécialiste reprendre
                    }
                    state.setCursor(nextPlayer(state, cursor));
                }
                case NOT_STARTED, DONE -> {
                    return;
                }
            }
        }
    }

    /** Le joueur qui agit : le rang courant du tour, dans l'ordre du tour de la manche. */
    private static int currentPlayer(GameState state) {
        return state.turnOrder().get(state.cursor().turnPosition());
    }

    /** Clôt 1a/cascade : joue l'effort (1b) automatique et ouvre la récupération (1c). */
    private static PreparationCursor effortThenRecover(GameState state, PreparationCursor cursor) {
        int player = state.turnOrder().get(cursor.turnPosition());
        Preparation.applyEffort(state.player(player));
        int budget = state.player(player).attributes().level(Attribute.COORDINATION);
        return new PreparationCursor(cursor.turnPosition(), Step.RECOVER, 0, cursor.pendingVessels(), budget);
    }

    /** Passe au joueur suivant du tour, ou clôt la phase après le dernier. */
    private static PreparationCursor nextPlayer(GameState state, PreparationCursor cursor) {
        int next = cursor.turnPosition() + 1;
        Step step = next >= state.playerCount() ? Step.DONE : Step.RECRUIT;
        return new PreparationCursor(next, step, 0, cursor.pendingVessels(), 0);
    }

    private static <T extends Action> T require(Action action, Class<T> type, Step step) {
        if (!type.isInstance(action)) {
            throw new IllegalStateException("L'étape " + step + " attend un coup " + type.getSimpleName());
        }
        return type.cast(action);
    }
}
