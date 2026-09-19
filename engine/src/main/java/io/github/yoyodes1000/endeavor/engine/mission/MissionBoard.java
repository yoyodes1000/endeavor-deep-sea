package io.github.yoyodes1000.endeavor.engine.mission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * L'état d'occupation du plateau Impact d'une partie : quels hexagones portent un
 * pion, et à qui. Le plateau lui-même ({@link ImpactBoard}) est une donnée
 * immuable ; c'est l'occupation qui évolue.
 *
 * <p>Objet d'état mutable + {@link #copy()} (décision 3).
 */
public final class MissionBoard {

    private final ImpactBoard board;
    private final List<MissionGoal> goals;
    private final Map<Long, Integer> ownerByPosition;

    /** Un plateau sans objectif de fin de mission (aucun point d'objectif au décompte final). */
    public MissionBoard(ImpactBoard board) {
        this(board, List.of());
    }

    public MissionBoard(ImpactBoard board, List<MissionGoal> goals) {
        if (board == null || goals == null) {
            throw new IllegalArgumentException("Le plateau de mission a besoin d'un plateau Impact et d'objectifs");
        }
        this.board = board;
        this.goals = List.copyOf(goals);
        this.ownerByPosition = new HashMap<>();
    }

    private MissionBoard(ImpactBoard board, List<MissionGoal> goals, Map<Long, Integer> owners) {
        this.board = board;
        this.goals = goals;
        this.ownerByPosition = new HashMap<>(owners);
    }

    /** Le plateau d'une mission : son plateau Impact et ses objectifs de fin de mission. */
    public static MissionBoard forMission(Mission mission) {
        return new MissionBoard(mission.impactBoard(), mission.goals());
    }

    /** Les objectifs de fin de mission, comptés au décompte final. */
    public List<MissionGoal> goals() {
        return goals;
    }

    public ImpactBoard board() {
        return board;
    }

    public boolean isOccupied(ImpactHex hex) {
        return ownerByPosition.containsKey(position(hex));
    }

    /** Le propriétaire du pion posé sur l'hexagone, s'il y en a un. */
    public OptionalInt owner(ImpactHex hex) {
        Integer owner = ownerByPosition.get(position(hex));
        return owner == null ? OptionalInt.empty() : OptionalInt.of(owner);
    }

    /**
     * Les hexagones où poser un pion est légal : sur la grille, libres, et soit
     * des cases de départ, soit voisines d'un hexagone occupé — quel qu'en soit
     * le propriétaire (décision 7).
     */
    public List<ImpactHex> legalPlacements() {
        List<ImpactHex> result = new ArrayList<>();
        for (ImpactHex hex : board.hexes()) {
            if (hex.offGrid() || isOccupied(hex)) {
                continue;
            }
            if (hex.start() || board.neighbors(hex).stream().anyMatch(this::isOccupied)) {
                result.add(hex);
            }
        }
        return result;
    }

    /**
     * Pose un pion du joueur sur l'hexagone (occupation seule ; la récompense est
     * résolue à part).
     *
     * @throws IllegalArgumentException si la pose n'est pas légale
     */
    public void place(ImpactHex hex, int playerIndex) {
        if (playerIndex < 0) {
            throw new IllegalArgumentException("Indice de joueur négatif : " + playerIndex);
        }
        if (!legalPlacements().contains(hex)) {
            throw new IllegalArgumentException("Pose illégale sur l'hexagone " + hex.row() + "," + hex.col());
        }
        ownerByPosition.put(position(hex), playerIndex);
    }

    /** Copie indépendante, pour isoler une simulation (déc. 3). */
    public MissionBoard copy() {
        return new MissionBoard(board, goals, ownerByPosition);
    }

    private static long position(ImpactHex hex) {
        return ((long) hex.row() << 32) ^ (hex.col() & 0xffffffffL);
    }
}
