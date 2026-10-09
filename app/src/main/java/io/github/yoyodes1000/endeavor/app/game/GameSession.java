package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.ai.Bot;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.play.Game;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Une partie en cours côté application : l'état du moteur, les bots des sièges
 * adverses, et le journal des coups joués depuis la mise en place. Avec la graine, ce
 * journal suffit à rejouer la partie à l'identique : c'est la matière de la future
 * sauvegarde.
 *
 * <p>Le joueur humain occupe le siège {@value #HUMAN_SEAT}. Les bots jouent dès que la
 * main leur revient : entre deux requêtes, c'est donc toujours à l'humain de jouer,
 * sauf une fois la partie finie.
 *
 * <p>Le coup du joueur et la réponse des bots s'appliquent sur une copie de l'état,
 * qui ne remplace l'original qu'une fois le tout réussi : une règle qui échoue en cours
 * de route ne laisse pas une partie à moitié modifiée.
 *
 * <p>Non thread-safe : {@link GameService} en sérialise l'accès.
 */
final class GameSession {

    static final int HUMAN_SEAT = 0;

    private final int missionNumber;
    private final long seed;
    private final Map<Integer, Bot> bots;
    private final List<Action> journal = new ArrayList<>();
    private List<PlayedMove> lastBotMoves;
    private GameState state;

    private GameSession(int missionNumber, GameState state, Map<Integer, Bot> bots) {
        Set<Integer> botSeats = IntStream.range(0, state.playerCount())
                .filter(seat -> seat != HUMAN_SEAT)
                .boxed()
                .collect(Collectors.toSet());
        if (!bots.keySet().equals(botSeats)) {
            throw new IllegalArgumentException("Il faut un bot par siège adverse, et seulement eux : " + botSeats);
        }
        this.missionNumber = missionNumber;
        this.seed = state.random().seed();
        this.bots = Map.copyOf(bots);
        this.state = state;
    }

    /**
     * Démarre une partie mise en place (non commencée), puis laisse jouer les bots
     * jusqu'à la première décision du joueur humain.
     */
    static GameSession begin(int missionNumber, GameState state, Map<Integer, Bot> bots) {
        GameSession session = new GameSession(missionNumber, state, bots);
        Game.begin(state);
        session.lastBotMoves = session.playBots(state);
        session.lastBotMoves.forEach(move -> session.journal.add(move.action()));
        return session;
    }

    /**
     * Joue le coup du joueur humain, puis ceux des bots jusqu'à ce que la main lui
     * revienne ou que la partie s'achève.
     *
     * @param moveNumber le numéro de coup de la vue sur laquelle le coup a été choisi
     * @throws StaleMoveException   si la partie a avancé depuis cette vue
     * @throws IllegalMoveException si ce n'est pas un coup légal du joueur humain
     */
    void play(int moveNumber, Action action) {
        if (moveNumber != journal.size()) {
            throw new StaleMoveException();
        }
        if (!isHumanTurn() || !Game.legalActions(state).contains(action)) {
            throw new IllegalMoveException();
        }
        GameState next = state.copy();
        Game.apply(next, action);
        List<PlayedMove> botMoves = playBots(next);

        state = next;
        journal.add(action);
        botMoves.forEach(move -> journal.add(move.action()));
        lastBotMoves = botMoves;
    }

    GameView view() {
        OptionalInt current = Game.currentPlayer(state);
        boolean yourTurn = isHumanTurn();
        return new GameView(missionNumber, journal.size(), state.round(), state.phase(), state.playerCount(),
                HUMAN_SEAT, current.isPresent() ? current.getAsInt() : null, yourTurn,
                yourTurn ? Game.legalActions(state) : List.of(), lastBotMoves);
    }

    /** @throws GameNotFinishedException si la partie n'est pas terminée */
    ResultView result() {
        if (state.phase() != GamePhase.FINISHED) {
            throw new GameNotFinishedException();
        }
        return ResultView.of(Game.finalResult(state));
    }

    /** La graine de la partie : avec le journal, elle permet de la rejouer. */
    long seed() {
        return seed;
    }

    /** Les coups joués depuis la mise en place, tous sièges confondus, dans l'ordre. */
    List<Action> journal() {
        return List.copyOf(journal);
    }

    private boolean isHumanTurn() {
        return Game.currentPlayer(state).equals(OptionalInt.of(HUMAN_SEAT));
    }

    /** Fait jouer les bots sur {@code on} tant que la main leur revient ; rend leurs coups. */
    private List<PlayedMove> playBots(GameState on) {
        List<PlayedMove> played = new ArrayList<>();
        OptionalInt seat = Game.currentPlayer(on);
        while (seat.isPresent() && seat.getAsInt() != HUMAN_SEAT) {
            int botSeat = seat.getAsInt();
            List<Action> legal = Game.legalActions(on);
            Action move = bots.get(botSeat).choose(on.viewFor(botSeat), legal);
            if (!legal.contains(move)) {
                throw new IllegalStateException("Le bot du siège " + botSeat + " a choisi un coup illégal : " + move);
            }
            Game.apply(on, move);
            played.add(new PlayedMove(botSeat, move));
            seat = Game.currentPlayer(on);
        }
        return List.copyOf(played);
    }
}
