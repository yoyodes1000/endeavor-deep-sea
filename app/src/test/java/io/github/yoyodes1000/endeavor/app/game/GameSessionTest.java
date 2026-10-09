package io.github.yoyodes1000.endeavor.app.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.yoyodes1000.endeavor.ai.Bot;
import io.github.yoyodes1000.endeavor.ai.RandomBot;
import io.github.yoyodes1000.endeavor.app.material.MaterialConfiguration;
import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PlayerView;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.play.Game;
import io.github.yoyodes1000.endeavor.engine.play.GameSetup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final MaterialConfiguration MATERIAL = new MaterialConfiguration();
    private static final GameSetup.Materials MATERIALS = MATERIAL.materials();
    private static final MissionCatalog MISSIONS = MATERIAL.missionCatalog();

    private static final int MISSION = 1;

    /** Garde-fou : une partie réelle tient en bien moins de coups humains que cela. */
    private static final int MAX_HUMAN_MOVES = 5_000;

    private static GameState newState(int players, long seed) {
        return GameSetup.newMissionGame(MISSIONS.byNumber(MISSION).orElseThrow(), players, MATERIALS,
                RandomSource.fromSeed(seed), GameService.STARTING_DISCS);
    }

    private static Map<Integer, Bot> randomBots(int players, long seed) {
        Map<Integer, Bot> bots = new HashMap<>();
        for (int seat = 1; seat < players; seat++) {
            bots.put(seat, new RandomBot(RandomSource.fromSeed(seed + seat)));
        }
        return bots;
    }

    private static GameSession newSession(int players, long seed) {
        return GameSession.begin(MISSION, newState(players, seed), randomBots(players, seed));
    }

    /** Le joueur humain joue toujours son premier coup légal, jusqu'à la fin. */
    private static void playToTheEnd(GameSession session) {
        int moves = 0;
        GameView view = session.view();
        while (view.phase() != GamePhase.FINISHED) {
            session.play(view.moveNumber(), view.legalActions().get(0));
            view = session.view();
            if (++moves > MAX_HUMAN_MOVES) {
                fail("la partie ne se termine pas");
            }
        }
    }

    @Test
    void auDemarrageLaMainEstAuJoueurHumain() {
        GameView view = newSession(3, 1).view();

        assertTrue(view.yourTurn());
        assertEquals(GameSession.HUMAN_SEAT, view.currentPlayer());
        assertFalse(view.legalActions().isEmpty());
        assertTrue(view.botMoves().stream().noneMatch(move -> move.seat() == GameSession.HUMAN_SEAT));
        assertEquals(view.botMoves().size(), view.moveNumber(), "seuls les bots ont pu jouer avant l'humain");
    }

    @Test
    void entreDeuxCoupsCEstToujoursAuJoueurHumainSaufPartieFinie() {
        GameSession session = newSession(4, 2);
        GameView view = session.view();
        int moves = 0;
        while (view.phase() != GamePhase.FINISHED) {
            assertTrue(view.yourTurn(), "les bots ont rendu la main");
            int before = view.moveNumber();
            session.play(before, view.legalActions().get(0));
            view = session.view();
            assertEquals(before + 1 + view.botMoves().size(), view.moveNumber(), "coup humain + coups des bots");
            if (++moves > MAX_HUMAN_MOVES) {
                fail("la partie ne se termine pas");
            }
        }

        assertFalse(view.yourTurn());
        assertNull(view.currentPlayer());
        assertTrue(view.legalActions().isEmpty());
        assertEquals(4, session.result().scores().size());
    }

    @Test
    void unCoupChoisiSurUnEtatPerimeEstRefuse() {
        GameSession session = newSession(2, 3);
        GameView view = session.view();

        assertThrows(StaleMoveException.class,
                () -> session.play(view.moveNumber() + 1, view.legalActions().get(0)));
        assertEquals(view, session.view(), "la partie n'a pas bougé");
    }

    @Test
    void unCoupIllegalEstRefuse() {
        GameSession session = newSession(2, 4);
        GameView view = session.view();

        assertThrows(IllegalMoveException.class, () -> session.play(view.moveNumber(), new Recruter("inconnu")));
        assertEquals(view, session.view(), "la partie n'a pas bougé");
    }

    @Test
    void leDecompteNExisteQuApresLaFin() {
        GameSession session = newSession(2, 5);
        assertThrows(GameNotFinishedException.class, session::result);

        playToTheEnd(session);
        assertEquals(2, session.result().scores().size());
    }

    @Test
    void laGraineEtLeJournalRejouentLaPartieALIdentique() {
        GameSession session = newSession(3, 6);
        playToTheEnd(session);

        GameState replay = newState(3, session.seed());
        Game.begin(replay);
        for (Action action : session.journal()) {
            Game.apply(replay, action);
        }

        assertEquals(GamePhase.FINISHED, replay.phase());
        assertEquals(session.result(), ResultView.of(Game.finalResult(replay)));
    }

    @Test
    void unePanneDeBotLaisseLaPartieIntacte() {
        BreakableBot bot = new BreakableBot(new RandomBot(RandomSource.fromSeed(7)));
        GameSession session = GameSession.begin(MISSION, newState(2, 7), Map.of(1, bot));
        bot.breakDown();

        int moves = 0;
        while (true) {
            GameView before = session.view();
            try {
                session.play(before.moveNumber(), before.legalActions().get(0));
            } catch (IllegalStateException breakdown) {
                assertEquals(before, session.view(), "ni le coup humain ni un coup de bot n'a été retenu");
                assertEquals(before.moveNumber(), session.journal().size());
                return;
            }
            if (++moves > MAX_HUMAN_MOVES) {
                fail("la main ne passe jamais au bot");
            }
        }
    }

    @Test
    void ilFautUnBotParSiegeAdverse() {
        assertThrows(IllegalArgumentException.class, () -> GameSession.begin(MISSION, newState(3, 8),
                randomBots(2, 8)));
        assertThrows(IllegalArgumentException.class, () -> GameSession.begin(MISSION, newState(2, 8),
                Map.of(0, new RandomBot(RandomSource.fromSeed(8)), 1, new RandomBot(RandomSource.fromSeed(9)))));
    }

    /** Un bot qui joue normalement, puis tombe en panne à la demande. */
    private static final class BreakableBot implements Bot {

        private final Bot delegate;
        private boolean broken;

        BreakableBot(Bot delegate) {
            this.delegate = delegate;
        }

        void breakDown() {
            broken = true;
        }

        @Override
        public Action choose(PlayerView view, List<Action> legalActions) {
            if (broken) {
                throw new IllegalStateException("panne simulée");
            }
            return delegate.choose(view, legalActions);
        }
    }
}
