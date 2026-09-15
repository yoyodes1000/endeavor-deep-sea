package io.github.yoyodes1000.endeavor.engine.activation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.action.Voyager;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.game.ActivationCursor;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionSlot;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Le driver de la Phase 2 (Activation) : la signature centrale (décision 1) pour
 * cette phase. Chacun à son tour, dans l'ordre du tour, jusqu'à ce que
 * <strong>tous aient passé</strong>.
 *
 * <p>Un tour standard : {@link Activer} un spécialiste (poser un disque de transit
 * sur sa case d'activation libre), puis exécuter tout ou partie de sa chaîne
 * d'actions, avant {@link TerminerTour} (rendre la main en restant dans la manche)
 * ou {@link Passer} (quitter la manche définitivement). La première action
 * concrète est le {@link Voyager} : déplacer un submersible d'une zone à une zone
 * accessible (portée et profondeur bornées par le niveau de technologie).
 *
 * <p>Le contexte de tour — spécialiste activé et avancement dans sa chaîne
 * d'emplacements — vit dans l'{@link ActivationCursor}, copié avec l'état pour
 * l'IA (déc. 2 et 3). Chaque {@code Voyager} résout l'emplacement courant et
 * avance d'un cran ; le joueur peut s'arrêter à tout moment (rien n'est
 * obligatoire). Le <strong>bonus d'arrivée</strong> d'un Voyage n'est pas encore
 * résolu ici — il viendra avec le catalogue de tuiles dans l'état et la cascade de
 * gains (brique sœur).
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
        int player = currentPlayer(state);
        List<Action> actions = new ArrayList<>();
        if (cursor.activatedThisTurn()) {
            actions.addAll(voyagesOf(state, player, cursor));
            actions.add(new TerminerTour());
            actions.add(new Passer());
        } else {
            actions.addAll(activationsOf(state, player));
            actions.add(new Passer());
        }
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
        int player = currentPlayer(state);
        switch (action) {
            case Activer activer -> {
                if (cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Un seul spécialiste peut être activé par tour");
                }
                state.player(player).activate(activer.specialistId());
                state.setActivationCursor(new ActivationCursor(
                        cursor.turnPosition(), cursor.passed(), activer.specialistId(), 0));
            }
            case Voyager voyager -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Voyage sans spécialiste activé");
                }
                requireTravelSlot(state, player, cursor);
                requireReachable(state, player, voyager);
                state.oceanBoard().moveVessel(voyager.from(), voyager.to(), player);
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                        cursor.activatedSpecialist(), cursor.actionStep() + 1));
            }
            case TerminerTour ignored -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Rien à terminer : agir ou passer");
                }
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, cursor.passed()), cursor.passed(), null, 0));
            }
            case Passer ignored -> {
                Set<Integer> passed = new HashSet<>(cursor.passed());
                passed.add(player);
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, passed), passed, null, 0));
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

    /**
     * Les Voyages légaux si l'emplacement d'action courant offre le voyage : pour
     * chaque zone où le joueur a un submersible, chaque destination accessible à son
     * niveau de technologie.
     */
    private static List<Voyager> voyagesOf(GameState state, int playerIndex, ActivationCursor cursor) {
        if (!currentSlotOffersTravel(state, playerIndex, cursor)) {
            return List.of();
        }
        OceanBoard ocean = state.oceanBoard();
        int level = state.player(playerIndex).attributes().level(Attribute.INGENUITY);
        List<Voyager> voyages = new ArrayList<>();
        for (Cell from : ocean.vesselCells(playerIndex)) {
            for (Cell to : ocean.reachableFrom(from, level)) {
                voyages.add(new Voyager(from, to));
            }
        }
        return voyages;
    }

    private static boolean currentSlotOffersTravel(GameState state, int playerIndex, ActivationCursor cursor) {
        return currentSlot(state, playerIndex, cursor)
                .map(slot -> slot.choices().contains(ActionType.TRAVEL))
                .orElse(false);
    }

    /** L'emplacement d'action courant de la chaîne du spécialiste activé, s'il en reste. */
    private static Optional<ActionSlot> currentSlot(GameState state, int playerIndex, ActivationCursor cursor) {
        List<ActionSlot> chain = activatedChain(state, playerIndex, cursor);
        int step = cursor.actionStep();
        return step < chain.size() ? Optional.of(chain.get(step)) : Optional.empty();
    }

    private static List<ActionSlot> activatedChain(GameState state, int playerIndex, ActivationCursor cursor) {
        for (HeldSpecialist held : state.player(playerIndex).specialists()) {
            if (held.specialist().id().equals(cursor.activatedSpecialist())) {
                return held.activeSide().actions();
            }
        }
        throw new IllegalStateException("Spécialiste activé introuvable : " + cursor.activatedSpecialist());
    }

    private static void requireTravelSlot(GameState state, int playerIndex, ActivationCursor cursor) {
        if (!currentSlotOffersTravel(state, playerIndex, cursor)) {
            throw new IllegalStateException("L'emplacement d'action courant n'offre pas de Voyage");
        }
    }

    private static void requireReachable(GameState state, int playerIndex, Voyager voyager) {
        int level = state.player(playerIndex).attributes().level(Attribute.INGENUITY);
        if (!state.oceanBoard().reachableFrom(voyager.from(), level).contains(voyager.to())) {
            throw new IllegalStateException("Destination hors de portée : " + voyager.to());
        }
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
