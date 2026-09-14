package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * L'état de l'océan d'une partie : quelles tuiles occupent la grille, et quels
 * submersibles s'y trouvent. La grille est indexée par {@link Cell}
 * (profondeur × colonne) et <strong>grandit</strong> en cours de partie (la
 * découverte pose de nouvelles tuiles).
 *
 * <p>Deux couches, comme le plateau Impact : le matériel des tuiles
 * ({@code OceanTile}) est immuable ; c'est cet {@code OceanBoard} qui porte
 * l'occupation, mutable, avec {@link #copy()} (décision 3).
 *
 * <p>L'<strong>adjacence se calcule</strong> à partir des coordonnées (décision
 * 7) : les voisins d'une case sont les quatre cases orthogonales qui portent une
 * tuile — une case vide n'est pas un voisin, ce qui interdit de « franchir un
 * vide ». Le déplacement (Voyage) s'appuie sur {@link #reachableFrom(Cell, int)}.
 *
 * <p>Ne sont modélisés ici que le placement et les submersibles : l'occupation
 * des sites par des disques viendra avec les actions qui l'écrivent.
 */
public final class OceanBoard {

    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 5;

    /** Voisinage orthogonal : au-dessus, en dessous, à gauche, à droite. */
    private static final int[][] DELTAS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final int columns;
    private final Map<Cell, String> tileByCell;
    private final Map<Cell, Map<Integer, Integer>> vesselsByCell;

    /** Un océan vide de {@code columns} colonnes ; les tuiles s'y posent ensuite. */
    public OceanBoard(int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("Un océan a au moins une colonne : " + columns);
        }
        this.columns = columns;
        this.tileByCell = new HashMap<>();
        this.vesselsByCell = new HashMap<>();
    }

    private OceanBoard(int columns, Map<Cell, String> tiles, Map<Cell, Map<Integer, Integer>> vessels) {
        this.columns = columns;
        this.tileByCell = new HashMap<>(tiles);
        this.vesselsByCell = new HashMap<>();
        for (Map.Entry<Cell, Map<Integer, Integer>> entry : vessels.entrySet()) {
            this.vesselsByCell.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }

    public int columns() {
        return columns;
    }

    /**
     * Pose une tuile sur une case libre de la grille (le geste de mise en place et
     * de découverte). La récompense associée est résolue à part.
     *
     * @throws IllegalArgumentException si la case est hors grille, déjà occupée, ou
     *     si l'identifiant de tuile est vide
     */
    public void placeTile(Cell cell, String tileId) {
        requireOnGrid(cell);
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("Une tuile posée doit avoir un identifiant");
        }
        if (tileByCell.containsKey(cell)) {
            throw new IllegalArgumentException("Case déjà occupée : " + cell);
        }
        tileByCell.put(cell, tileId);
    }

    /** Vrai si une tuile occupe cette case (une zone y est en jeu). */
    public boolean isOccupied(Cell cell) {
        return tileByCell.containsKey(cell);
    }

    /** L'identifiant de la tuile posée sur cette case, s'il y en a une. */
    public Optional<String> tileAt(Cell cell) {
        return Optional.ofNullable(tileByCell.get(cell));
    }

    /**
     * Ajoute des submersibles d'un joueur dans une zone (mise en jeu, arrivée d'un
     * Voyage). Une zone peut en porter plusieurs, de joueurs différents.
     *
     * @throws IllegalArgumentException si la case ne porte pas de tuile, si le
     *     joueur ou le nombre est invalide
     */
    public void addVessels(Cell cell, int playerIndex, int count) {
        if (playerIndex < 0) {
            throw new IllegalArgumentException("Indice de joueur négatif : " + playerIndex);
        }
        if (count < 0) {
            throw new IllegalArgumentException("Nombre de submersibles négatif : " + count);
        }
        if (!isOccupied(cell)) {
            throw new IllegalArgumentException("Aucune zone où poser un submersible : " + cell);
        }
        if (count == 0) {
            return;
        }
        vesselsByCell.computeIfAbsent(cell, ignored -> new HashMap<>()).merge(playerIndex, count, Integer::sum);
    }

    /** Combien de submersibles ce joueur a dans cette zone. */
    public int vesselCount(Cell cell, int playerIndex) {
        Map<Integer, Integer> perPlayer = vesselsByCell.get(cell);
        return perPlayer == null ? 0 : perPlayer.getOrDefault(playerIndex, 0);
    }

    /**
     * Déplace un submersible du joueur d'une zone vers une autre (le pas
     * élémentaire d'un Voyage). La légalité du trajet — portée, profondeur, vide
     * infranchissable — est l'affaire de {@link #reachableFrom(Cell, int)} ; ici on
     * ne vérifie que la présence du submersible et l'existence d'une zone d'arrivée.
     *
     * @throws IllegalArgumentException si le joueur n'a pas de submersible au départ,
     *     ou si la destination ne porte pas de tuile
     */
    public void moveVessel(Cell from, Cell to, int playerIndex) {
        if (vesselCount(from, playerIndex) == 0) {
            throw new IllegalArgumentException("Aucun submersible à déplacer en " + from);
        }
        if (!isOccupied(to)) {
            throw new IllegalArgumentException("Destination sans zone : " + to);
        }
        Map<Integer, Integer> perFrom = vesselsByCell.get(from);
        int remaining = perFrom.get(playerIndex) - 1;
        if (remaining == 0) {
            perFrom.remove(playerIndex);
        } else {
            perFrom.put(playerIndex, remaining);
        }
        if (perFrom.isEmpty()) {
            vesselsByCell.remove(from);
        }
        addVessels(to, playerIndex, 1);
    }

    /** Les zones voisines occupées (adjacence orthogonale, vides exclus). */
    public List<Cell> neighbors(Cell cell) {
        List<Cell> result = new ArrayList<>();
        for (int[] delta : DELTAS) {
            int depth = cell.depth() + delta[0];
            int col = cell.col() + delta[1];
            if (depth < MIN_DEPTH || depth > MAX_DEPTH || col < 0 || col >= columns) {
                continue;
            }
            Cell neighbor = new Cell(depth, col);
            if (isOccupied(neighbor)) {
                result.add(neighbor);
            }
        }
        return result;
    }

    /**
     * Les zones qu'un submersible partant de {@code origin} peut atteindre par un
     * Voyage, le niveau de technologie bornant <strong>à la fois</strong> la
     * distance (nombre de pas) et la profondeur atteinte. Parcours en largeur sur
     * les seules cases occupées — on ne franchit jamais un vide. L'origine est
     * exclue du résultat : un Voyage doit déplacer.
     *
     * @param level le niveau de technologie (≥ 0) ; à 0, aucune destination
     * @throws IllegalArgumentException si le niveau est négatif
     */
    public Set<Cell> reachableFrom(Cell origin, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Niveau de technologie négatif : " + level);
        }
        Set<Cell> reachable = new LinkedHashSet<>();
        if (level == 0 || !isOccupied(origin)) {
            return reachable;
        }
        Map<Cell, Integer> distance = new HashMap<>();
        distance.put(origin, 0);
        Deque<Cell> queue = new ArrayDeque<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            int step = distance.get(current);
            if (step == level) {
                continue;
            }
            for (Cell neighbor : neighbors(current)) {
                if (neighbor.depth() > level || distance.containsKey(neighbor)) {
                    continue;
                }
                distance.put(neighbor, step + 1);
                reachable.add(neighbor);
                queue.add(neighbor);
            }
        }
        return reachable;
    }

    /** Copie indépendante, pour isoler une simulation (décision 3). */
    public OceanBoard copy() {
        return new OceanBoard(columns, tileByCell, vesselsByCell);
    }

    private void requireOnGrid(Cell cell) {
        if (cell.depth() > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "Profondeur hors de " + MIN_DEPTH + ".." + MAX_DEPTH + " : " + cell.depth());
        }
        if (cell.col() >= columns) {
            throw new IllegalArgumentException(
                    "Colonne hors des " + columns + " colonnes : " + cell.col());
        }
    }
}
