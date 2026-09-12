package io.github.yoyodes1000.endeavor.engine.mission;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Le plateau Impact d'une mission : une grille d'hexagones décrite en données,
 * dont l'<strong>adjacence se calcule</strong> à partir des coordonnées, jamais
 * stockée (décision 7). La pose d'un pion impact exige une case de départ ou le
 * voisin d'un hexagone occupé — d'où le besoin du graphe d'adjacence.
 *
 * <p>Adjacence implémentée pour {@code pointy-top} et {@code flat-top} ;
 * {@code rect-rows} (mission 10) reste à faire. La convention de décalage exacte
 * (odd-r / odd-q) est un choix documenté : la connexité ne la départage pas, elle
 * se vérifiera à l'affichage.
 *
 * <p>Les hexagones {@code offGrid} (∞ hors-grille) sont exclus de l'adjacence
 * géométrique : ils s'atteignent par une flèche, traitée à part.
 */
public final class ImpactBoard {

    private final HexOrientation orientation;
    private final List<ImpactHex> hexes;
    private final Map<Long, ImpactHex> gridByPosition;

    public ImpactBoard(HexOrientation orientation, List<ImpactHex> hexes) {
        if (orientation == null) {
            throw new IllegalArgumentException("Le plateau Impact a une orientation");
        }
        if (hexes == null || hexes.isEmpty()) {
            throw new IllegalArgumentException("Le plateau Impact est vide");
        }
        this.orientation = orientation;
        this.hexes = List.copyOf(hexes);

        Map<Long, ImpactHex> grid = new HashMap<>();
        for (ImpactHex hex : this.hexes) {
            if (hex.offGrid()) {
                continue;
            }
            if (grid.put(position(hex.row(), hex.col()), hex) != null) {
                throw new IllegalArgumentException(
                        "Deux hexagones à la même position : " + hex.row() + "," + hex.col());
            }
        }
        this.gridByPosition = grid;
    }

    public HexOrientation orientation() {
        return orientation;
    }

    public List<ImpactHex> hexes() {
        return hexes;
    }

    /** L'hexagone (de la grille) à cette position, s'il existe. */
    public Optional<ImpactHex> hexAt(int row, int col) {
        return Optional.ofNullable(gridByPosition.get(position(row, col)));
    }

    /** Les cases de départ (où l'on peut poser sans voisin occupé). */
    public List<ImpactHex> startHexes() {
        return hexes.stream().filter(ImpactHex::start).toList();
    }

    /** Les hexagones voisins présents sur la grille (adjacence géométrique). */
    public List<ImpactHex> neighbors(ImpactHex hex) {
        List<ImpactHex> result = new ArrayList<>();
        for (int[] delta : deltas(hex.row(), hex.col())) {
            hexAt(hex.row() + delta[0], hex.col() + delta[1]).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Les hexagones atteignables depuis les cases de départ, de proche en proche.
     * Sert de contrôle de relevé (décision 7) : une case mal placée devient presque
     * toujours inaccessible.
     */
    public Set<ImpactHex> reachableFromStarts() {
        Set<ImpactHex> seen = new HashSet<>(startHexes());
        Deque<ImpactHex> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty()) {
            for (ImpactHex neighbor : neighbors(queue.poll())) {
                if (seen.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return Collections.unmodifiableSet(seen);
    }

    private int[][] deltas(int row, int col) {
        return switch (orientation) {
            case POINTY_TOP -> Math.floorMod(row, 2) == 0
                    ? new int[][] {{0, 1}, {0, -1}, {-1, 0}, {-1, -1}, {1, 0}, {1, -1}}
                    : new int[][] {{0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {1, 1}, {1, 0}};
            case FLAT_TOP -> Math.floorMod(col, 2) == 0
                    ? new int[][] {{1, 0}, {-1, 0}, {0, -1}, {-1, -1}, {0, 1}, {-1, 1}}
                    : new int[][] {{1, 0}, {-1, 0}, {1, -1}, {0, -1}, {1, 1}, {0, 1}};
            case RECT_ROWS -> throw new UnsupportedOperationException(
                    "Adjacence rect-rows à implémenter (mission 10)");
        };
    }

    private static long position(int row, int col) {
        return ((long) row << 32) ^ (col & 0xffffffffL);
    }
}
