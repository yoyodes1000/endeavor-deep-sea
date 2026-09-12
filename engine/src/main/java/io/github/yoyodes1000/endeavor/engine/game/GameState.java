package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'état complet d'une partie : tous les joueurs, le casier partagé des tuiles
 * recrutables, l'ordre du tour et la source d'aléa.
 *
 * <p>C'est la <strong>source unique de vérité</strong> du jeu (décision 2) : il
 * contient tout, y compris ce qu'un joueur donné ne voit pas. Ce qu'un joueur
 * observe passe par une {@link PlayerView}, jamais par cet objet — c'est ce qui,
 * plus tard, empêchera mécaniquement l'IA de tricher.
 *
 * <p>Objet mutable copié une fois par simulation (décision 3).
 */
public final class GameState {

    private static final int FIRST_ROUND = 1;

    private final List<Player> players;
    private final List<Specialist> casier;
    private final RandomSource random;
    private int firstPlayerIndex;
    private int round;

    private GameState(List<Player> players, List<Specialist> casier, RandomSource random,
                      int firstPlayerIndex, int round) {
        this.players = players;
        this.casier = casier;
        this.random = random;
        this.firstPlayerIndex = firstPlayerIndex;
        this.round = round;
    }

    /**
     * Prépare une partie neuve : chaque joueur démarre avec le chef d'équipe, le
     * casier reçoit toutes les tuiles recrutables (tout sauf les chefs d'équipe),
     * et on se place au début de la première manche.
     *
     * <p>Le premier joueur (tirage de la manche 1) et la mise en place fine sont
     * du ressort des étapes dédiées ; ici, l'indice du premier joueur vaut 0 en
     * attendant que l'étape « premier joueur » le fixe.
     *
     * @param playerCount   nombre de joueurs, au moins 1
     * @param startingDiscs disques d'action de départ par joueur
     */
    public static GameState newGame(int playerCount, SpecialistRoster roster,
                                    RandomSource random, int startingDiscs) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Il faut au moins un joueur : " + playerCount);
        }
        if (roster == null || random == null) {
            throw new IllegalArgumentException("Le casier et la source d'aléa sont requis");
        }
        Specialist teamLeader = roster.teamLeader();
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(Player.start(teamLeader, startingDiscs));
        }
        List<Specialist> casier = new ArrayList<>();
        for (Specialist specialist : roster.specialists()) {
            if (!specialist.teamLeader()) {
                casier.add(specialist);
            }
        }
        return new GameState(players, casier, random, 0, FIRST_ROUND);
    }

    public int playerCount() {
        return players.size();
    }

    public Player player(int index) {
        return players.get(index);
    }

    public List<Player> players() {
        return Collections.unmodifiableList(players);
    }

    /** Le casier partagé des tuiles recrutables. */
    public List<Specialist> casier() {
        return Collections.unmodifiableList(casier);
    }

    public int firstPlayerIndex() {
        return firstPlayerIndex;
    }

    public int round() {
        return round;
    }

    /** La source d'aléa de la partie (déterminisme : graine + journal). */
    public RandomSource random() {
        return random;
    }

    /** La vue de la partie telle que l'observe le joueur d'indice donné (déc. 2). */
    public PlayerView viewFor(int playerIndex) {
        return new PlayerView(this, playerIndex);
    }

    /** Copie indépendante, appelée une fois par simulation pour l'isoler (déc. 3). */
    public GameState copy() {
        List<Player> playersCopy = new ArrayList<>(players.size());
        for (Player player : players) {
            playersCopy.add(player.copy());
        }
        return new GameState(playersCopy, new ArrayList<>(casier), random.copy(), firstPlayerIndex, round);
    }
}
