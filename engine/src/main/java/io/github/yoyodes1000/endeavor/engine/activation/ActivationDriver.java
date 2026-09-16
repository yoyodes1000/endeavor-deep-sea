package io.github.yoyodes1000.endeavor.engine.activation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.GarderTuile;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.PoserTuile;
import io.github.yoyodes1000.endeavor.engine.action.Sonar;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.action.Voyager;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.GainResolver;
import io.github.yoyodes1000.endeavor.engine.effect.ImpactPlacement;
import io.github.yoyodes1000.endeavor.engine.game.ActivationCursor;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PendingDiscovery;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionSlot;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Le driver de la Phase 2 (Activation) : la signature centrale (décision 1) pour
 * cette phase. Chacun à son tour, dans l'ordre du tour, jusqu'à ce que
 * <strong>tous aient passé</strong>.
 *
 * <p>Un tour standard : {@link Activer} un spécialiste (poser un disque de transit
 * sur sa case d'activation libre), puis exécuter tout ou partie de sa chaîne
 * d'actions, avant {@link TerminerTour} (rendre la main en restant dans la manche)
 * ou {@link Passer} (quitter la manche définitivement). La première action
 * concrète est le {@link Voyager} : déplacer un submersible d'une zone à une zone
 * accessible (portée et profondeur bornées par le niveau de technologie), puis
 * encaisser le <strong>bonus d'arrivée</strong> de la zone de destination.
 *
 * <p>Le {@link Sonar} suit : dépenser un disque de transit pour le poser sur la
 * case libre la plus à gauche d'une piste Sonar d'une zone où le joueur a un
 * submersible. Une case <strong>récompense</strong> encaisse ses gains (via le
 * résolveur et la même cascade d'impacts) ; une case <strong>découverte</strong>
 * pioche deux tuiles et suspend le tour sur deux décisions — {@link GarderTuile}
 * (garder l'une, l'autre retourne à la pioche) puis {@link PoserTuile} (poser la
 * gardée sur une case valide, ce qui encaisse son bonus de découverte).
 *
 * <p>Résoudre un bonus d'arrivée peut relancer une <strong>cascade</strong> :
 * les submersibles gagnés rejoignent le stock du joueur, et chaque pion impact
 * gagné doit être posé ({@link PoserImpact}) avant de poursuivre le tour — le
 * même mécanisme qu'en préparation, réutilisé via {@link ImpactPlacement}.
 *
 * <p>Le contexte de tour — spécialiste activé, avancement dans sa chaîne, impacts
 * en attente — vit dans l'{@link ActivationCursor}, copié avec l'état (déc. 2 et 3).
 */
public final class ActivationDriver {

    /** Une découverte fait piocher deux tuiles, dont une est gardée. */
    private static final int DISCOVERY_DRAW = 2;

    private ActivationDriver() {
    }

    /** Entre en Phase 2 : premier joueur du tour, personne n'a encore passé. */
    public static void begin(GameState state) {
        state.setActivationCursor(ActivationCursor.notStarted());
    }

    /** Vrai quand tous les joueurs ont passé : la Phase 2 de la manche est finie. */
    public static boolean isDone(GameState state) {
        return state.activationCursor().passed().size() == state.playerCount();
    }

    /** Les coups légaux du joueur courant (vide si la phase est finie). */
    public static List<Action> legalActions(GameState state) {
        if (isDone(state)) {
            return List.of();
        }
        if (state.activationCursor().discovering()) {
            return discoveryActions(state);
        }
        if (mustPlaceImpact(state)) {
            return impactPlacements(state);
        }
        ActivationCursor cursor = state.activationCursor();
        int player = currentPlayer(state);
        List<Action> actions = new ArrayList<>();
        if (cursor.activatedThisTurn()) {
            actions.addAll(voyagesOf(state, player, cursor));
            actions.addAll(sonarsOf(state, player, cursor));
            actions.add(new TerminerTour());
            actions.add(new Passer());
        } else {
            actions.addAll(activationsOf(state, player));
            actions.add(new Passer());
        }
        return actions;
    }

    /**
     * Applique un coup et fait avancer le tour ou le round-robin.
     *
     * @throws IllegalStateException si la phase est finie ou le coup inattendu à ce
     *     stade du tour
     */
    public static void apply(GameState state, Action action) {
        if (isDone(state)) {
            throw new IllegalStateException("La phase d'activation est terminée");
        }
        ActivationCursor cursor = state.activationCursor();
        int player = currentPlayer(state);
        if (cursor.discovering()) {
            applyDiscovery(state, player, cursor, action);
            return;
        }
        if (mustPlaceImpact(state)) {
            placePendingImpact(state, player, cursor, action);
            return;
        }
        switch (action) {
            case Activer activer -> {
                if (cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Un seul spécialiste peut être activé par tour");
                }
                state.player(player).activate(activer.specialistId());
                state.setActivationCursor(new ActivationCursor(
                        cursor.turnPosition(), cursor.passed(), activer.specialistId(), 0, 0, null));
            }
            case Voyager voyager -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Voyage sans spécialiste activé");
                }
                requireSlotOffers(state, player, cursor, ActionType.TRAVEL);
                requireReachable(state, player, voyager);
                state.oceanBoard().moveVessel(voyager.from(), voyager.to(), player);
                EffectOutcome arrival = resolveArrival(state, player, voyager.to());
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                        cursor.activatedSpecialist(), cursor.actionStep() + 1, arrival.impactsEarned(), null));
            }
            case Sonar sonar -> applySonar(state, player, cursor, sonar);
            case TerminerTour ignored -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Rien à terminer : agir ou passer");
                }
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, cursor.passed()), cursor.passed(), null, 0, 0, null));
            }
            case Passer ignored -> {
                Set<Integer> passed = new HashSet<>(cursor.passed());
                passed.add(player);
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, passed), passed, null, 0, 0, null));
            }
            default -> throw new IllegalStateException("Coup inattendu en activation : " + action);
        }
    }

    /**
     * Applique un Sonar : dépenser un disque de transit, le poser sur la case libre
     * la plus à gauche de la piste, puis résoudre cette case. Une <strong>récompense</strong>
     * encaisse ses gains ; une <strong>découverte</strong> tire deux tuiles et suspend le
     * tour sur le choix de celle à garder (le reste de la découverte s'enchaîne ensuite).
     */
    private static void applySonar(GameState state, int player, ActivationCursor cursor, Sonar sonar) {
        if (!cursor.activatedThisTurn()) {
            throw new IllegalStateException("Sonar sans spécialiste activé");
        }
        requireSlotOffers(state, player, cursor, ActionType.SONAR);
        SonarSpot spot = requireSonarSpot(state, player, sonar);
        if (exhaustedDiscovery(state, spot)) {
            throw new IllegalStateException("Pioche épuisée pour cette découverte (règle limite à venir)");
        }
        state.player(player).spendTransitDisc();
        state.oceanBoard().placeSonarDisc(sonar.cell(), sonar.trackIndex(), player);
        switch (spot) {
            case SonarSpot.Reward reward -> {
                EffectOutcome outcome = GainResolver.resolve(state.player(player), reward.gains());
                state.player(player).gainVessels(outcome.vesselsEarned());
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                        cursor.activatedSpecialist(), cursor.actionStep() + 1, outcome.impactsEarned(), null));
            }
            case SonarSpot.Discover discover -> {
                List<String> drawn = state.discoveryPile().draw(
                        DISCOVERY_DRAW, Set.copyOf(discover.levels()), state.random());
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                        cursor.activatedSpecialist(), cursor.actionStep(), 0, PendingDiscovery.toChooseFrom(drawn)));
            }
        }
    }

    /** Vrai s'il reste un impact à poser et de la place pour le faire (le tour est suspendu). */
    private static boolean mustPlaceImpact(GameState state) {
        return state.activationCursor().pendingImpacts() > 0
                && !state.missionBoard().legalPlacements().isEmpty();
    }

    private static List<Action> impactPlacements(GameState state) {
        return state.missionBoard().legalPlacements().stream()
                .map(hex -> (Action) new PoserImpact(hex.row(), hex.col()))
                .toList();
    }

    private static void placePendingImpact(GameState state, int player, ActivationCursor cursor, Action action) {
        if (!(action instanceof PoserImpact placement)) {
            throw new IllegalStateException("Un impact reste à poser avant de poursuivre le tour");
        }
        ImpactHex hex = state.missionBoard().board().hexAt(placement.row(), placement.col())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun hexagone en " + placement.row() + "," + placement.col()));
        EffectOutcome outcome = ImpactPlacement.place(state.missionBoard(), state.player(player), hex, player);
        state.player(player).gainVessels(outcome.vesselsEarned());
        int stillPending = cursor.pendingImpacts() - 1 + outcome.impactsEarned();
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep(), stillPending, null));
    }

    /** Les coups légaux d'une découverte en cours : choisir une tuile, puis la poser. */
    private static List<Action> discoveryActions(GameState state) {
        PendingDiscovery pending = state.activationCursor().pendingDiscovery();
        if (pending.awaitingChoice()) {
            return pending.candidates().stream()
                    .map(tileId -> (Action) new GarderTuile(tileId))
                    .toList();
        }
        return validPlacements(state, pending.keptTile()).stream()
                .map(cell -> (Action) new PoserTuile(cell))
                .toList();
    }

    /**
     * Applique un coup pendant une découverte : d'abord garder l'une des tuiles
     * piochées (l'autre retourne à la pioche), puis poser la tuile gardée sur une
     * case valide, ce qui encaisse son bonus de découverte et clôt le pas de Sonar.
     */
    private static void applyDiscovery(GameState state, int player, ActivationCursor cursor, Action action) {
        PendingDiscovery pending = cursor.pendingDiscovery();
        if (pending.awaitingChoice()) {
            if (!(action instanceof GarderTuile keep)) {
                throw new IllegalStateException("Une tuile de découverte reste à choisir");
            }
            if (!pending.candidates().contains(keep.tileId())) {
                throw new IllegalStateException("Tuile hors des candidates de découverte : " + keep.tileId());
            }
            for (String candidate : pending.candidates()) {
                if (!candidate.equals(keep.tileId())) {
                    state.discoveryPile().returnTile(candidate);
                }
            }
            if (validPlacements(state, keep.tileId()).isEmpty()) {
                throw new IllegalStateException(
                        "Aucune pose valide pour " + keep.tileId() + " (règle « aire pleine → 1 impact » à venir)");
            }
            state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                    cursor.activatedSpecialist(), cursor.actionStep(), 0, pending.kept(keep.tileId())));
            return;
        }
        if (!(action instanceof PoserTuile placement)) {
            throw new IllegalStateException("La tuile de découverte reste à poser");
        }
        requireValidPlacement(state, pending.keptTile(), placement.cell());
        state.oceanBoard().placeTile(placement.cell(), pending.keptTile());
        EffectOutcome outcome = resolveDiscoverBonus(state, player, pending.keptTile());
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep() + 1, outcome.impactsEarned(), null));
    }

    /**
     * Les cases où poser une tuile de découverte : une case libre de la profondeur
     * de la tuile, et — hors niveau 1 — sous une zone déjà en jeu (la case juste
     * au-dessus est occupée).
     */
    private static List<Cell> validPlacements(GameState state, String tileId) {
        int depth = tileById(state, tileId).depth();
        OceanBoard ocean = state.oceanBoard();
        List<Cell> cells = new ArrayList<>();
        for (int col = 0; col < ocean.columns(); col++) {
            Cell cell = new Cell(depth, col);
            if (ocean.isOccupied(cell)) {
                continue;
            }
            if (depth == 1 || ocean.isOccupied(new Cell(depth - 1, col))) {
                cells.add(cell);
            }
        }
        return cells;
    }

    private static void requireValidPlacement(GameState state, String tileId, Cell cell) {
        if (!validPlacements(state, tileId).contains(cell)) {
            throw new IllegalStateException("Pose de tuile de découverte invalide en " + cell);
        }
    }

    /** Encaisse le bonus de découverte d'une tuile fraîchement posée (gains + submersibles). */
    private static EffectOutcome resolveDiscoverBonus(GameState state, int player, String tileId) {
        OceanTile tile = tileById(state, tileId);
        EffectOutcome outcome = GainResolver.resolve(state.player(player), tile.discoverBonus());
        state.player(player).gainVessels(outcome.vesselsEarned());
        return outcome;
    }

    /**
     * Encaisse le bonus d'arrivée de la zone de destination : applique ses gains
     * directs, verse les submersibles gagnés au stock, et renvoie les impacts gagnés
     * (à poser par la cascade).
     */
    private static EffectOutcome resolveArrival(GameState state, int player, Cell destination) {
        OceanTile tile = tileAt(state, destination);
        EffectOutcome outcome = GainResolver.resolve(state.player(player), tile.arrivalBonus());
        state.player(player).gainVessels(outcome.vesselsEarned());
        return outcome;
    }

    /** La tuile posée sur une zone, résolue via le catalogue. */
    private static OceanTile tileAt(GameState state, Cell cell) {
        String tileId = state.oceanBoard().tileAt(cell).orElseThrow(
                () -> new IllegalStateException("Zone sans tuile : " + cell));
        return tileById(state, tileId);
    }

    /** La tuile d'identifiant donné dans le catalogue. */
    private static OceanTile tileById(GameState state, String tileId) {
        return state.oceanTileCatalog().byId(tileId).orElseThrow(
                () -> new IllegalStateException("Tuile inconnue au catalogue : " + tileId));
    }

    /** Les activations légales du joueur : une par spécialiste à case libre, s'il a un disque en transit. */
    private static List<Activer> activationsOf(GameState state, int playerIndex) {
        Player player = state.player(playerIndex);
        if (player.transitDiscs() == 0) {
            return List.of();
        }
        List<Activer> activations = new ArrayList<>();
        for (HeldSpecialist held : player.specialists()) {
            if (held.placedDiscs() == 0) {
                activations.add(new Activer(held.specialist().id()));
            }
        }
        return activations;
    }

    /**
     * Les Voyages légaux si l'emplacement d'action courant offre le voyage : pour
     * chaque zone où le joueur a un submersible, chaque destination accessible à son
     * niveau de technologie.
     */
    private static List<Voyager> voyagesOf(GameState state, int playerIndex, ActivationCursor cursor) {
        if (!currentSlotOffers(state, playerIndex, cursor, ActionType.TRAVEL)) {
            return List.of();
        }
        OceanBoard ocean = state.oceanBoard();
        int level = state.player(playerIndex).attributes().level(Attribute.INGENUITY);
        List<Voyager> voyages = new ArrayList<>();
        for (Cell from : ocean.vesselCells(playerIndex)) {
            for (Cell to : ocean.reachableFrom(from, level)) {
                voyages.add(new Voyager(from, to));
            }
        }
        return voyages;
    }

    /**
     * Les Sonars légaux si l'emplacement d'action courant offre le sonar et qu'il
     * reste un disque en transit à poser : pour chaque zone où le joueur a un
     * submersible, chaque piste qui a encore une case libre — récompense (gains
     * immédiats) ou découverte (piocher et poser une tuile).
     */
    private static List<Sonar> sonarsOf(GameState state, int playerIndex, ActivationCursor cursor) {
        if (!currentSlotOffers(state, playerIndex, cursor, ActionType.SONAR)) {
            return List.of();
        }
        if (state.player(playerIndex).transitDiscs() == 0) {
            return List.of();
        }
        OceanBoard ocean = state.oceanBoard();
        List<Sonar> sonars = new ArrayList<>();
        for (Cell cell : ocean.vesselCells(playerIndex)) {
            List<SonarTrack> tracks = tileAt(state, cell).sonarTracks();
            for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
                SonarSpot spot = leftmostFreeSpot(tracks.get(trackIndex),
                        ocean.sonarDiscCount(cell, trackIndex)).orElse(null);
                if (spot != null && !exhaustedDiscovery(state, spot)) {
                    sonars.add(new Sonar(cell, trackIndex));
                }
            }
        }
        return sonars;
    }

    /** Vrai si la case est une découverte dont les niveaux n'ont plus de tuile à piocher. */
    private static boolean exhaustedDiscovery(GameState state, SonarSpot spot) {
        return spot instanceof SonarSpot.Discover discover
                && state.discoveryPile().availableAtLevels(Set.copyOf(discover.levels())).isEmpty();
    }

    /** La case libre la plus à gauche d'une piste, ou vide si la piste est pleine. */
    private static Optional<SonarSpot> leftmostFreeSpot(SonarTrack track, int filled) {
        return filled < track.spots().size() ? Optional.of(track.spots().get(filled)) : Optional.empty();
    }

    private static boolean currentSlotOffers(GameState state, int playerIndex, ActivationCursor cursor,
                                             ActionType action) {
        return currentSlot(state, playerIndex, cursor)
                .map(slot -> slot.choices().contains(action))
                .orElse(false);
    }

    /** L'emplacement d'action courant de la chaîne du spécialiste activé, s'il en reste. */
    private static Optional<ActionSlot> currentSlot(GameState state, int playerIndex, ActivationCursor cursor) {
        List<ActionSlot> chain = activatedChain(state, playerIndex, cursor);
        int step = cursor.actionStep();
        return step < chain.size() ? Optional.of(chain.get(step)) : Optional.empty();
    }

    private static List<ActionSlot> activatedChain(GameState state, int playerIndex, ActivationCursor cursor) {
        for (HeldSpecialist held : state.player(playerIndex).specialists()) {
            if (held.specialist().id().equals(cursor.activatedSpecialist())) {
                return held.activeSide().actions();
            }
        }
        throw new IllegalStateException("Spécialiste activé introuvable : " + cursor.activatedSpecialist());
    }

    private static void requireSlotOffers(GameState state, int playerIndex, ActivationCursor cursor,
                                          ActionType action) {
        if (!currentSlotOffers(state, playerIndex, cursor, action)) {
            throw new IllegalStateException("L'emplacement d'action courant n'offre pas : " + action);
        }
    }

    private static void requireReachable(GameState state, int playerIndex, Voyager voyager) {
        int level = state.player(playerIndex).attributes().level(Attribute.INGENUITY);
        if (!state.oceanBoard().reachableFrom(voyager.from(), level).contains(voyager.to())) {
            throw new IllegalStateException("Destination hors de portée : " + voyager.to());
        }
    }

    /**
     * Résout la case libre la plus à gauche de la piste visée — récompense ou
     * découverte. Vérifie au passage la présence d'un submersible et l'existence de
     * la piste.
     *
     * @throws IllegalStateException si le joueur n'a pas de submersible dans la zone,
     *     ou si la piste n'existe pas ou est pleine
     */
    private static SonarSpot requireSonarSpot(GameState state, int playerIndex, Sonar sonar) {
        if (state.oceanBoard().vesselCount(sonar.cell(), playerIndex) == 0) {
            throw new IllegalStateException("Aucun submersible dans la zone du Sonar : " + sonar.cell());
        }
        List<SonarTrack> tracks = tileAt(state, sonar.cell()).sonarTracks();
        if (sonar.trackIndex() >= tracks.size()) {
            throw new IllegalStateException(
                    "Piste Sonar inexistante en " + sonar.cell() + " : " + sonar.trackIndex());
        }
        int filled = state.oceanBoard().sonarDiscCount(sonar.cell(), sonar.trackIndex());
        return leftmostFreeSpot(tracks.get(sonar.trackIndex()), filled).orElseThrow(
                () -> new IllegalStateException("Piste Sonar déjà pleine : " + sonar.cell()));
    }

    private static int currentPlayer(GameState state) {
        return state.turnOrder().get(state.activationCursor().turnPosition());
    }

    /**
     * Le rang du prochain joueur qui n'a pas passé, en tournant depuis le rang
     * courant. Si tous ont passé, on garde le rang courant (la phase est finie).
     */
    private static int nextActivePosition(GameState state, ActivationCursor cursor, Set<Integer> passed) {
        int count = state.playerCount();
        if (passed.size() >= count) {
            return cursor.turnPosition();
        }
        int position = cursor.turnPosition();
        do {
            position = (position + 1) % count;
        } while (passed.contains(state.turnOrder().get(position)));
        return position;
    }
}
