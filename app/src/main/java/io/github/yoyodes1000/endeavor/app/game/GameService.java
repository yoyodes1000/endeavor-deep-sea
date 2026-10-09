package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.ai.Bot;
import io.github.yoyodes1000.endeavor.ai.RandomBot;
import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.play.GameSetup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * La partie en cours de l'application : une seule à la fois, gardée en mémoire —
 * l'application est locale et n'a qu'un joueur. Les accès sont sérialisés, deux
 * onglets ne peuvent donc pas jouer en même temps sur le même état.
 *
 * <p>Le joueur humain affronte des bots aléatoires ({@link RandomBot}) en attendant
 * l'IA Monte-Carlo.
 */
@Service
public class GameService {

    /** Disques d'action de départ par joueur. Valeur provisoire, à confirmer sur le livret. */
    static final int STARTING_DISCS = 6;

    static final int MIN_OPPONENTS = 1;
    static final int MAX_OPPONENTS = 3;

    /** Décale la graine de chaque bot de celle de la partie, pour que leurs tirages ne se recoupent pas. */
    private static final long BOT_SEED_STRIDE = 0x2545F4914F6CDD1DL;

    private final MissionCatalog missions;
    private final GameSetup.Materials materials;
    private GameSession current;

    public GameService(MissionCatalog missions, GameSetup.Materials materials) {
        this.missions = missions;
        this.materials = materials;
    }

    /** Les missions qu'on peut lancer : celles dont la fiche désigne une zone de lancement. */
    public List<MissionSummary> playableMissions() {
        return missions.missions().stream()
                .filter(Mission::hasLaunchZone)
                .map(MissionSummary::of)
                .toList();
    }

    /**
     * Lance une nouvelle partie, qui remplace la précédente.
     *
     * @throws InvalidGameRequestException si la mission n'est pas jouable ou le nombre
     *     d'adversaires hors bornes
     */
    public synchronized GameView start(NewGameRequest request) {
        Mission mission = playableMission(request.mission());
        int opponents = validOpponents(request.opponents());
        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();

        GameState state = GameSetup.newMissionGame(mission, opponents + 1, materials, RandomSource.fromSeed(seed),
                STARTING_DISCS);
        current = GameSession.begin(mission.number(), state, randomBots(seed, opponents));
        return current.view();
    }

    /** @throws NoGameInProgressException si aucune partie n'a été lancée */
    public synchronized GameView view() {
        return requireGame().view();
    }

    /**
     * Joue un coup du joueur humain et la réponse des bots.
     *
     * @throws InvalidGameRequestException si le numéro de coup ou le coup manque
     * @throws StaleMoveException          si la partie a avancé depuis la vue du client
     * @throws IllegalMoveException        si le coup n'est pas légal
     */
    public synchronized GameView play(MoveRequest request) {
        if (request.moveNumber() == null || request.action() == null) {
            throw new InvalidGameRequestException("Un coup porte son numéro et son action");
        }
        GameSession game = requireGame();
        game.play(request.moveNumber(), request.action());
        return game.view();
    }

    /** @throws GameNotFinishedException si la partie n'est pas terminée */
    public synchronized ResultView result() {
        return requireGame().result();
    }

    private GameSession requireGame() {
        if (current == null) {
            throw new NoGameInProgressException();
        }
        return current;
    }

    private Mission playableMission(Integer number) {
        if (number == null) {
            throw new InvalidGameRequestException("La mission est requise");
        }
        return missions.byNumber(number)
                .filter(Mission::hasLaunchZone)
                .orElseThrow(() -> new InvalidGameRequestException("Mission inconnue ou pas encore jouable"));
    }

    private static int validOpponents(Integer opponents) {
        if (opponents == null || opponents < MIN_OPPONENTS || opponents > MAX_OPPONENTS) {
            throw new InvalidGameRequestException(
                    "Le nombre d'adversaires va de " + MIN_OPPONENTS + " à " + MAX_OPPONENTS);
        }
        return opponents;
    }

    private static Map<Integer, Bot> randomBots(long seed, int opponents) {
        Map<Integer, Bot> bots = new HashMap<>();
        for (int seat = GameSession.HUMAN_SEAT + 1; seat <= opponents; seat++) {
            bots.put(seat, new RandomBot(RandomSource.fromSeed(seed + BOT_SEED_STRIDE * seat)));
        }
        return bots;
    }
}
