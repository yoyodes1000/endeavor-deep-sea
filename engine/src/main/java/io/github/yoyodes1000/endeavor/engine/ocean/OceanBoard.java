package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
 * <p>Première occupation d'un site par des disques : les <strong>pistes
 * Sonar</strong>, remplies de gauche à droite (l'action Sonar). Le board ne fait
 * qu'enregistrer les disques posés ({@link #placeSonarDisc}, {@link
 * #sonarDiscCount}) ; c'est l'appelant qui a le catalogue des tuiles et qui juge
 * la légalité de la pose — comme le déplacement s'en remet à {@link
 * #reachableFrom(Cell, int)}. Même patron pour les <strong>sites de plongée</strong> :
 * une pile de jetons empilée à la création de la zone ({@link #stackDiveTokens}),
 * dont l'action Plongée prend le sommet ({@link #takeDiveToken}). Les
 * <strong>sites de conservation</strong> suivent le même patron mais à un seul
 * disque, jamais repris ({@link #placeConservationDisc}, {@link
 * #conservationSiteOccupied}) — comme les <strong>sites de publication</strong>
 * ({@link #placeJournalDisc}, {@link #journalSiteOccupied}). L'occupation des
 * autres sites viendra avec les actions qui l'écrivent.
 */
public final class OceanBoard {

    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 5;

    /** Voisinage orthogonal : au-dessus, en dessous, à gauche, à droite. */
    private static final int[][] DELTAS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    /** Une piste Sonar identifiée sur la grille : sa case et son rang sur la tuile. */
    private record SonarTrackKey(Cell cell, int trackIndex) {
    }

    /** Un site de plongée identifié sur la grille : sa case et son identifiant sur la tuile. */
    private record DiveSiteKey(Cell cell, String siteId) {
    }

    /** Un site de conservation identifié sur la grille : sa case et son identifiant sur la tuile. */
    private record ConservationSiteKey(Cell cell, String siteId) {
    }

    /** Un site de publication identifié sur la grille : sa case et son identifiant sur la tuile. */
    private record JournalSiteKey(Cell cell, String siteId) {
    }

    private final int columns;
    private final Map<Cell, String> tileByCell;
    private final Map<Cell, Map<Integer, Integer>> vesselsByCell;
    private final Map<SonarTrackKey, List<Integer>> sonarDiscsByTrack;
    private final Map<DiveSiteKey, Deque<String>> diveTokensBySite;
    private final Map<ConservationSiteKey, Integer> conservationOccupantBySite;
    private final Map<JournalSiteKey, Integer> journalOccupantBySite;

    /** Un océan vide de {@code columns} colonnes ; les tuiles s'y posent ensuite. */
    public OceanBoard(int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("Un océan a au moins une colonne : " + columns);
        }
        this.columns = columns;
        this.tileByCell = new HashMap<>();
        this.vesselsByCell = new HashMap<>();
        this.sonarDiscsByTrack = new HashMap<>();
        this.diveTokensBySite = new HashMap<>();
        this.conservationOccupantBySite = new HashMap<>();
        this.journalOccupantBySite = new HashMap<>();
    }

    private OceanBoard(int columns, Map<Cell, String> tiles, Map<Cell, Map<Integer, Integer>> vessels,
                       Map<SonarTrackKey, List<Integer>> sonarDiscs, Map<DiveSiteKey, Deque<String>> diveTokens,
                       Map<ConservationSiteKey, Integer> conservationOccupants,
                       Map<JournalSiteKey, Integer> journalOccupants) {
        this.columns = columns;
        this.tileByCell = new HashMap<>(tiles);
        this.vesselsByCell = new HashMap<>();
        for (Map.Entry<Cell, Map<Integer, Integer>> entry : vessels.entrySet()) {
            this.vesselsByCell.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        this.sonarDiscsByTrack = new HashMap<>();
        for (Map.Entry<SonarTrackKey, List<Integer>> entry : sonarDiscs.entrySet()) {
            this.sonarDiscsByTrack.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.diveTokensBySite = new HashMap<>();
        for (Map.Entry<DiveSiteKey, Deque<String>> entry : diveTokens.entrySet()) {
            this.diveTokensBySite.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
        }
        this.conservationOccupantBySite = new HashMap<>(conservationOccupants);
        this.journalOccupantBySite = new HashMap<>(journalOccupants);
    }

    /**
     * Construit l'océan de départ d'une mission depuis sa mise en place : pose les
     * tuiles nommées, et tire les tuiles aléatoires ({@code randomLevel}) dans la
     * pile du niveau demandé — via la {@link RandomSource} injectée, jamais d'aléa
     * implicite, pour que la même graine rebâtisse le même océan.
     *
     * <p>La pile d'un niveau exclut les tuiles <strong>uniques</strong> (réservées
     * à une mise en place de scénario) et toute tuile déjà posée par la mise en
     * place. Chargement <strong>strict</strong> (décision 4) : une tuile nommée
     * inconnue ou une pile épuisée fait échouer la construction.
     *
     * @throws IllegalArgumentException si un argument est nul, si une tuile nommée
     *     est absente du catalogue, ou si un tirage n'a aucun candidat
     */
    public static OceanBoard fromSetup(OceanSetup setup, OceanTileCatalog catalog, RandomSource random) {
        if (setup == null || catalog == null || random == null) {
            throw new IllegalArgumentException("La mise en place, le catalogue et la source d'aléa sont requis");
        }
        OceanBoard board = new OceanBoard(setup.columns());
        Set<String> used = new HashSet<>();
        for (StartingTile tile : setup.startingTiles()) {
            if (tile instanceof StartingTile.Named named) {
                used.add(named.tileId());
            }
        }
        for (StartingTile tile : setup.startingTiles()) {
            String tileId = switch (tile) {
                case StartingTile.Named named -> {
                    if (catalog.byId(named.tileId()).isEmpty()) {
                        throw new IllegalArgumentException("Tuile de mise en place inconnue : " + named.tileId());
                    }
                    yield named.tileId();
                }
                case StartingTile.Random drawn -> drawFromPile(catalog, random, drawn.level(), used);
            };
            board.placeTile(new Cell(tile.depth(), tile.col()), tileId);
            used.add(tileId);
        }
        return board;
    }

    private static String drawFromPile(OceanTileCatalog catalog, RandomSource random, int level, Set<String> used) {
        List<OceanTile> pile = catalog.ofDepth(level).stream()
                .filter(tile -> !tile.unique())
                .filter(tile -> !used.contains(tile.id()))
                .toList();
        if (pile.isEmpty()) {
            throw new IllegalArgumentException("Aucune tuile de niveau " + level + " disponible pour le tirage");
        }
        return pile.get(random.nextInt(pile.size())).id();
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

    /** Les identifiants des tuiles actuellement en jeu sur la grille. */
    public Set<String> placedTileIds() {
        return new HashSet<>(tileByCell.values());
    }

    /** Les cases occupées par une tuile, dans un ordre déterministe (profondeur puis colonne). */
    public List<Cell> occupiedCells() {
        return tileByCell.keySet().stream()
                .sorted(Comparator.comparingInt(Cell::depth).thenComparingInt(Cell::col))
                .toList();
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

    /**
     * Les zones où ce joueur a au moins un submersible, ordonnées (profondeur puis
     * colonne) pour une génération de coups déterministe.
     */
    public List<Cell> vesselCells(int playerIndex) {
        return vesselsByCell.entrySet().stream()
                .filter(entry -> entry.getValue().getOrDefault(playerIndex, 0) > 0)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(Cell::depth).thenComparingInt(Cell::col))
                .toList();
    }

    /**
     * Combien de disques sont déjà posés sur cette piste Sonar — donc l'indice de
     * la case libre la plus à gauche (0 = aucune case occupée).
     *
     * @throws IllegalArgumentException si le rang de piste est négatif
     */
    public int sonarDiscCount(Cell cell, int trackIndex) {
        requireTrackIndex(trackIndex);
        List<Integer> owners = sonarDiscsByTrack.get(new SonarTrackKey(cell, trackIndex));
        return owners == null ? 0 : owners.size();
    }

    /**
     * Pose le disque d'un joueur sur la case libre la plus à gauche d'une piste
     * Sonar. Le board ne connaît pas la forme de la piste (nombre de cases, type) :
     * il fait confiance à l'appelant, qui a le catalogue, pour n'appeler qu'une pose
     * légale — comme {@link #moveVessel} s'en remet à {@link #reachableFrom}.
     *
     * @throws IllegalArgumentException si le joueur ou le rang de piste est invalide,
     *     ou si la case ne porte pas de tuile
     */
    public void placeSonarDisc(Cell cell, int trackIndex, int playerIndex) {
        requireTrackIndex(trackIndex);
        if (playerIndex < 0) {
            throw new IllegalArgumentException("Indice de joueur négatif : " + playerIndex);
        }
        if (!isOccupied(cell)) {
            throw new IllegalArgumentException("Aucune zone où poser un disque de Sonar : " + cell);
        }
        sonarDiscsByTrack.computeIfAbsent(new SonarTrackKey(cell, trackIndex), ignored -> new ArrayList<>())
                .add(playerIndex);
    }

    /** Les joueurs ayant un disque sur cette piste Sonar, dans l'ordre de pose (décompte final). */
    public List<Integer> sonarDiscOwners(Cell cell, int trackIndex) {
        requireTrackIndex(trackIndex);
        List<Integer> owners = sonarDiscsByTrack.get(new SonarTrackKey(cell, trackIndex));
        return owners == null ? List.of() : List.copyOf(owners);
    }

    private void requireTrackIndex(int trackIndex) {
        if (trackIndex < 0) {
            throw new IllegalArgumentException("Rang de piste Sonar négatif : " + trackIndex);
        }
    }

    /**
     * Empile les jetons de plongée d'un site fraîchement en jeu (mise en place ou
     * découverte). L'ordre de la liste fixe l'empilement — index 0 devient le
     * sommet, premier pris. Le board ne connaît pas le nombre attendu (le
     * {@code tokenCount} du site) : c'est à l'appelant de fournir le bon compte,
     * comme pour {@link #placeSonarDisc}.
     *
     * @throws IllegalArgumentException si la case ne porte pas de tuile, ou si le
     *     site est déjà empilé (un site n'est créé qu'une fois)
     */
    public void stackDiveTokens(Cell cell, String siteId, List<String> tokenIds) {
        requireSiteId(siteId);
        if (!isOccupied(cell)) {
            throw new IllegalArgumentException("Aucune zone où empiler des jetons de plongée : " + cell);
        }
        DiveSiteKey key = new DiveSiteKey(cell, siteId);
        if (diveTokensBySite.containsKey(key)) {
            throw new IllegalArgumentException("Site de plongée déjà empilé : " + cell + "/" + siteId);
        }
        diveTokensBySite.put(key, new ArrayDeque<>(tokenIds));
    }

    /** Combien de jetons restent empilés sur ce site (0 si le site n'a pas encore été empilé). */
    public int diveTokenCount(Cell cell, String siteId) {
        requireSiteId(siteId);
        Deque<String> stack = diveTokensBySite.get(new DiveSiteKey(cell, siteId));
        return stack == null ? 0 : stack.size();
    }

    /**
     * Prend le jeton du sommet d'un site de plongée (l'action Plongée) et le
     * retire de la pile.
     *
     * @throws IllegalArgumentException si le site est vide ou n'a jamais été empilé
     */
    public String takeDiveToken(Cell cell, String siteId) {
        requireSiteId(siteId);
        Deque<String> stack = diveTokensBySite.get(new DiveSiteKey(cell, siteId));
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Site de plongée vide : " + cell + "/" + siteId);
        }
        return stack.removeFirst();
    }

    private void requireSiteId(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Un site de plongée doit avoir un identifiant");
        }
    }

    /** Vrai si un disque occupe déjà ce site de conservation. */
    public boolean conservationSiteOccupied(Cell cell, String siteId) {
        return conservationOccupant(cell, siteId).isPresent();
    }

    /** Le joueur qui occupe ce site de conservation, s'il y en a un (décompte final). */
    public Optional<Integer> conservationOccupant(Cell cell, String siteId) {
        requireConservationSiteId(siteId);
        return Optional.ofNullable(conservationOccupantBySite.get(new ConservationSiteKey(cell, siteId)));
    }

    /**
     * Pose le disque d'un joueur sur un site de conservation (l'action
     * Conservation) — un site n'accueille qu'un seul disque, jamais repris.
     *
     * @throws IllegalArgumentException si le joueur est invalide, si la case ne
     *     porte pas de tuile, ou si le site est déjà occupé
     */
    public void placeConservationDisc(Cell cell, String siteId, int playerIndex) {
        requireConservationSiteId(siteId);
        if (playerIndex < 0) {
            throw new IllegalArgumentException("Indice de joueur négatif : " + playerIndex);
        }
        if (!isOccupied(cell)) {
            throw new IllegalArgumentException("Aucune zone où poser un disque de conservation : " + cell);
        }
        ConservationSiteKey key = new ConservationSiteKey(cell, siteId);
        if (conservationOccupantBySite.containsKey(key)) {
            throw new IllegalArgumentException("Site de conservation déjà occupé : " + cell + "/" + siteId);
        }
        conservationOccupantBySite.put(key, playerIndex);
    }

    private void requireConservationSiteId(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Un site de conservation doit avoir un identifiant");
        }
    }

    /** Vrai si un disque occupe déjà ce site de publication. */
    public boolean journalSiteOccupied(Cell cell, String siteId) {
        return journalOccupant(cell, siteId).isPresent();
    }

    /** Le joueur qui occupe ce site de publication, s'il y en a un (décompte final). */
    public Optional<Integer> journalOccupant(Cell cell, String siteId) {
        requireJournalSiteId(siteId);
        return Optional.ofNullable(journalOccupantBySite.get(new JournalSiteKey(cell, siteId)));
    }

    /**
     * Pose le disque d'un joueur sur un site de publication (l'action
     * Publication) — un site n'accueille qu'un seul disque, jamais repris.
     *
     * @throws IllegalArgumentException si le joueur est invalide, si la case ne
     *     porte pas de tuile, ou si le site est déjà occupé
     */
    public void placeJournalDisc(Cell cell, String siteId, int playerIndex) {
        requireJournalSiteId(siteId);
        if (playerIndex < 0) {
            throw new IllegalArgumentException("Indice de joueur négatif : " + playerIndex);
        }
        if (!isOccupied(cell)) {
            throw new IllegalArgumentException("Aucune zone où poser un disque de publication : " + cell);
        }
        JournalSiteKey key = new JournalSiteKey(cell, siteId);
        if (journalOccupantBySite.containsKey(key)) {
            throw new IllegalArgumentException("Site de publication déjà occupé : " + cell + "/" + siteId);
        }
        journalOccupantBySite.put(key, playerIndex);
    }

    private void requireJournalSiteId(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("Un site de publication doit avoir un identifiant");
        }
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
        return new OceanBoard(columns, tileByCell, vesselsByCell, sonarDiscsByTrack, diveTokensBySite,
                conservationOccupantBySite, journalOccupantBySite);
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
