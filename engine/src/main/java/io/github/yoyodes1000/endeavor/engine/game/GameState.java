package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
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
    private final OceanBoard oceanBoard;
    private final OceanTileCatalog oceanTileCatalog;
    private final RandomSource random;
    private int firstPlayerIndex;
    private int round;
    private GamePhase phase;
    private PreparationCursor cursor;
    private ActivationCursor activationCursor;

    private GameState(List<Player> players, List<Specialist> casier, MissionBoard missionBoard,
                      OceanBoard oceanBoard, OceanTileCatalog oceanTileCatalog, RandomSource random,
                      int firstPlayerIndex, int round, GamePhase phase, PreparationCursor cursor,
                      ActivationCursor activationCursor) {
        this.players = players;
        this.casier = casier;
        this.missionBoard = missionBoard;
        this.oceanBoard = oceanBoard;
        this.oceanTileCatalog = oceanTileCatalog;
        this.random = random;
        this.firstPlayerIndex = firstPlayerIndex;
        this.round = round;
        this.phase = phase;
        this.cursor = cursor;
        this.activationCursor = activationCursor;
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
     * @param oceanBoard    l'océan de départ de la mission (grille et submersibles)
     * @param oceanTileCatalog le catalogue des tuiles Océan (matériel immuable, pour
     *                      retrouver les gains d'une tuile à l'arrivée d'un Voyage)
     */
    public static GameState newGame(int playerCount, SpecialistRoster roster,
                                    RandomSource random, int startingDiscs, MissionBoard missionBoard,
                                    OceanBoard oceanBoard, OceanTileCatalog oceanTileCatalog) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Il faut au moins un joueur : " + playerCount);
        }
        if (roster == null || random == null || missionBoard == null || oceanBoard == null
                || oceanTileCatalog == null) {
            throw new IllegalArgumentException(
                    "Le casier, la source d'aléa, le plateau de mission, l'océan et son catalogue sont requis");
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
        return new GameState(players, casier, missionBoard, oceanBoard, oceanTileCatalog, random, 0,
                FIRST_ROUND, GamePhase.PREPARATION, PreparationCursor.notStarted(), ActivationCursor.notStarted());
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

    /** L'océan de la partie : la grille des zones et les submersibles qui s'y trouvent. */
    public OceanBoard oceanBoard() {
        return oceanBoard;
    }

    /** Le catalogue des tuiles Océan (matériel immuable, partagé entre les copies). */
    public OceanTileCatalog oceanTileCatalog() {
        return oceanTileCatalog;
    }

    public int firstPlayerIndex() {
        return firstPlayerIndex;
    }

    public int round() {
        return round;
    }

    /** Vrai si la manche courante est la dernière de la partie. */
    public boolean isLastRound() {
        return round == LAST_ROUND;
    }

    /** La phase courante de la manche (préparation, activation, ou partie finie). */
    public GamePhase phase() {
        return phase;
    }

    /** Fixe la phase courante (réservé à la façade du moteur). */
    public void setPhase(GamePhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("La phase ne peut être nulle");
        }
        this.phase = phase;
    }

    /** La position de la partie dans la Phase 1 (le driver de préparation la fait avancer). */
    public PreparationCursor cursor() {
        return cursor;
    }

    /** Fixe le curseur de préparation (réservé au driver du moteur). */
    public void setCursor(PreparationCursor cursor) {
        if (cursor == null) {
            throw new IllegalArgumentException("Le curseur ne peut être nul");
        }
        this.cursor = cursor;
    }

    /** La position de la partie dans la Phase 2 (le driver d'activation la fait avancer). */
    public ActivationCursor activationCursor() {
        return activationCursor;
    }

    /** Fixe le curseur d'activation (réservé au driver du moteur). */
    public void setActivationCursor(ActivationCursor activationCursor) {
        if (activationCursor == null) {
            throw new IllegalArgumentException("Le curseur d'activation ne peut être nul");
        }
        this.activationCursor = activationCursor;
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
        return new GameState(playersCopy, new ArrayList<>(casier), missionBoard.copy(), oceanBoard.copy(),
                oceanTileCatalog, random.copy(), firstPlayerIndex, round, phase, cursor, activationCursor);
    }
}
