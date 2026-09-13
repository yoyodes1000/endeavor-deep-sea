package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private static final int LAST_ROUND = 6;

    private final List<Player> players;
    private final List<Specialist> casier;
    private final MissionBoard missionBoard;
    private final RandomSource random;
    private int firstPlayerIndex;
    private int round;

    private GameState(List<Player> players, List<Specialist> casier, MissionBoard missionBoard,
                      RandomSource random, int firstPlayerIndex, int round) {
        this.players = players;
        this.casier = casier;
        this.missionBoard = missionBoard;
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
     * @param missionBoard  le plateau Impact de la mission jouée (occupation vide)
     */
    public static GameState newGame(int playerCount, SpecialistRoster roster,
                                    RandomSource random, int startingDiscs, MissionBoard missionBoard) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Il faut au moins un joueur : " + playerCount);
        }
        if (roster == null || random == null || missionBoard == null) {
            throw new IllegalArgumentException("Le casier, la source d'aléa et le plateau de mission sont requis");
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
        return new GameState(players, casier, missionBoard, random, 0, FIRST_ROUND);
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

    /** Le plateau Impact de la mission et son occupation (où sont posés les pions). */
    public MissionBoard missionBoard() {
        return missionBoard;
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

    /** Fixe l'indice du premier joueur de la manche (validé). */
    public void setFirstPlayerIndex(int index) {
        if (index < 0 || index >= players.size()) {
            throw new IllegalArgumentException("Indice de premier joueur hors bornes : " + index);
        }
        this.firstPlayerIndex = index;
    }

    /** L'ordre du tour de la manche : les joueurs à partir du premier, en tournant. */
    public List<Integer> turnOrder() {
        List<Integer> order = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            order.add((firstPlayerIndex + i) % players.size());
        }
        return order;
    }

    /** Passe à la manche suivante (la partie en compte six). */
    public void enterNextRound() {
        if (round >= LAST_ROUND) {
            throw new IllegalStateException("La partie ne compte que " + LAST_ROUND + " manches");
        }
        round++;
    }

    /**
     * Retire du casier la tuile d'identifiant donné et la renvoie (recrutement).
     *
     * @throws IllegalArgumentException si aucune tuile ne porte cet identifiant
     */
    public Specialist removeFromCasier(String specialistId) {
        for (Iterator<Specialist> it = casier.iterator(); it.hasNext(); ) {
            Specialist specialist = it.next();
            if (specialist.id().equals(specialistId)) {
                it.remove();
                return specialist;
            }
        }
        throw new IllegalArgumentException("Tuile absente du casier : " + specialistId);
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
        return new GameState(playersCopy, new ArrayList<>(casier), missionBoard.copy(),
                random.copy(), firstPlayerIndex, round);
    }
}
