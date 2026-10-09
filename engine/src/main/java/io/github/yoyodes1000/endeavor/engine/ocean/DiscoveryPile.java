package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * La pioche des tuiles Océan encore à découvrir : les tuiles non encore en jeu,
 * dans lesquelles le Sonar pioche (l'action Découverte). Pièce d'état mutable,
 * copiée avec la partie (déc. 3), pour qu'une même graine rejoue les mêmes tirages.
 *
 * <p>Contrairement à {@link OceanBoard#fromSetup} qui tire les tuiles de départ
 * dans une pile locale et éphémère, cette pioche <strong>persiste</strong> tout au
 * long de la partie : construite au démarrage à partir des tuiles non uniques
 * absentes du plateau, elle se vide au fil des découvertes.
 *
 * <p>Modèle simplifié (déc. cœur PR-B) : chaque niveau de profondeur n'est pas une
 * pile ordonnée distincte ; on tire uniformément parmi les tuiles disponibles aux
 * niveaux demandés. « Remettre une tuile sous sa pile » revient donc à la rendre
 * simplement disponible de nouveau ({@link #returnTile}).
 */
public final class DiscoveryPile {

    private final List<String> available;
    private final Map<String, Integer> depthById;

    private DiscoveryPile(List<String> available, Map<String, Integer> depthById) {
        this.available = new ArrayList<>(available);
        this.depthById = new HashMap<>(depthById);
    }

    /**
     * La pioche de départ d'une partie : toutes les tuiles non uniques du catalogue
     * qui ne sont pas déjà posées sur le plateau (mise en place comprise). Les tuiles
     * uniques sont réservées aux scénarios, jamais découvertes.
     */
    public static DiscoveryPile forGame(OceanTileCatalog catalog, OceanBoard board) {
        if (catalog == null || board == null) {
            throw new IllegalArgumentException("Le catalogue et le plateau sont requis");
        }
        Set<String> placed = board.placedTileIds();
        List<String> available = new ArrayList<>();
        Map<String, Integer> depthById = new HashMap<>();
        for (OceanTile tile : catalog.tiles()) {
            if (tile.unique() || placed.contains(tile.id())) {
                continue;
            }
            available.add(tile.id());
            depthById.put(tile.id(), tile.depth());
        }
        return new DiscoveryPile(available, depthById);
    }

    /** Les identifiants encore disponibles dont la profondeur figure parmi {@code levels}. */
    public List<String> availableAtLevels(Set<Integer> levels) {
        List<String> matching = new ArrayList<>();
        for (String tileId : available) {
            if (levels.contains(depthById.get(tileId))) {
                matching.add(tileId);
            }
        }
        return matching;
    }

    /** Les profondeurs où il reste au moins une tuile à découvrir, de la moins profonde à la plus profonde. */
    public Set<Integer> availableDepths() {
        Set<Integer> depths = new TreeSet<>();
        for (String tileId : available) {
            depths.add(depthById.get(tileId));
        }
        return depths;
    }

    /**
     * Tire jusqu'à {@code count} tuiles distinctes parmi les niveaux demandés, les
     * retire de la pioche et les renvoie. En renvoie moins si la pioche n'en a pas
     * assez (le cas « pioche épuisée » relève d'une règle limite à venir).
     *
     * @throws IllegalArgumentException si {@code count} est négatif ou la source nulle
     */
    public List<String> draw(int count, Set<Integer> levels, RandomSource random) {
        if (count < 0) {
            throw new IllegalArgumentException("Nombre de tuiles à tirer négatif : " + count);
        }
        if (random == null) {
            throw new IllegalArgumentException("La source d'aléa est requise");
        }
        List<String> candidates = availableAtLevels(levels);
        List<String> drawn = new ArrayList<>();
        for (int i = 0; i < count && !candidates.isEmpty(); i++) {
            String tileId = candidates.remove(random.nextInt(candidates.size()));
            available.remove(tileId);
            drawn.add(tileId);
        }
        return drawn;
    }

    /** Rend une tuile piochée à la pioche (la candidate non gardée retourne sous sa pile). */
    public void returnTile(String tileId) {
        if (!depthById.containsKey(tileId)) {
            throw new IllegalArgumentException("Tuile étrangère à la pioche : " + tileId);
        }
        if (!available.contains(tileId)) {
            available.add(tileId);
        }
    }

    /** Combien de tuiles restent à découvrir. */
    public int size() {
        return available.size();
    }

    /**
     * Glisse une tuile dans la pioche de son niveau (tuile cachée de mise en place : une
     * tuile unique n'y figure pas d'ordinaire). Sans effet si elle y est déjà.
     */
    public void addTile(String tileId, int depth) {
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("Une tuile ajoutée à la pioche doit avoir un identifiant");
        }
        if (available.contains(tileId)) {
            return;
        }
        depthById.put(tileId, depth);
        available.add(tileId);
    }

    /** Retire de la pioche les tuiles plus profondes que {@code maxDepth} (profondeur maximale d'une mission). */
    public void removeDeeperThan(int maxDepth) {
        available.removeIf(tileId -> depthById.get(tileId) > maxDepth);
    }

    /** Copie indépendante, pour isoler une simulation (déc. 3). */
    public DiscoveryPile copy() {
        return new DiscoveryPile(available, depthById);
    }
}
