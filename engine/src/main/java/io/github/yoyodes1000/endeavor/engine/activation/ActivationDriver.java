package io.github.yoyodes1000.endeavor.engine.activation;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.ChoisirAttribut;
import io.github.yoyodes1000.endeavor.engine.action.Conserver;
import io.github.yoyodes1000.endeavor.engine.action.DepenserJeton;
import io.github.yoyodes1000.endeavor.engine.action.Dive;
import io.github.yoyodes1000.endeavor.engine.action.GarderTuile;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.PoserTuile;
import io.github.yoyodes1000.endeavor.engine.action.Promouvoir;
import io.github.yoyodes1000.endeavor.engine.action.Publier;
import io.github.yoyodes1000.endeavor.engine.action.Sonar;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.action.Voyager;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.dive.DiveOption;
import io.github.yoyodes1000.endeavor.engine.dive.DiveSiteSetup;
import io.github.yoyodes1000.endeavor.engine.dive.DiveToken;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.GainResolver;
import io.github.yoyodes1000.endeavor.engine.effect.ImpactPlacement;
import io.github.yoyodes1000.endeavor.engine.game.ActivationCursor;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PendingDiscovery;
import io.github.yoyodes1000.endeavor.engine.journal.Journal;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.JournalSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionSlot;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

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
 * <p>Le {@link Dive} suit sans coût : prendre le jeton du sommet d'un site de
 * plongée d'une zone où le joueur a un submersible. Le jeton rejoint la main du
 * joueur, non résolu ; {@link DepenserJeton} en choisit l'option plus tard —
 * <strong>hors chaîne</strong>, à tout point de décision normal du tour, en plus
 * ou à la place de l'activation. Si l'option choisie accorde une action (Sonar,
 * Voyage, Plongée, Conservation), le tour se suspend sur cette seule action
 * ({@link ActivationCursor#pendingTokenAction()}) jusqu'à ce qu'elle soit jouée —
 * les mêmes coups {@link Sonar}/{@link Voyager}/{@link Dive}/{@link Conserver}/
 * {@link Publier} que la chaîne, mais sans en exiger l'activation ni
 * l'emplacement courant. Au plus un jeton peut rester en main à la fin du tour
 * ({@link TerminerTour}/{@link Passer}).
 *
 * <p>La {@link Conserver} suit à son tour : payer le coût en recherche du site
 * visé, dépenser un disque de transit pour l'y poser, puis encaisser ses gains.
 * Un site n'accueille qu'un seul disque, jamais repris.
 *
 * <p>La {@link Publier} suit : payer le coût en recherche de la revue à
 * l'étude visée, dépenser un disque de transit pour le poser sur un site dont
 * le symbole de domaine correspond, encaisser les gains du site puis ceux de
 * la revue — pour le joueur actif et pour chacun de ses adversaires — avant de
 * retirer la revue du marché (réassorti depuis la pioche).
 *
 * <p>La {@link Promouvoir} — un <strong>gain</strong> ({@code promote}) comme un
 * autre, encaissé au fil d'une résolution (site, revue, jeton) ou choisi comme
 * emplacement d'action dédié d'une chaîne — retourne une tuile Junior détenue
 * côté Senior : le disque posé dessus est perdu, la nouvelle case Senior repart
 * libre, ses gains immédiats sont encaissés à leur tour. Sans Junior détenu
 * (hors chef d'équipe, jamais promouvable), le gain est simplement perdu ; avec
 * au moins un Junior, un coup {@link Promouvoir} explicite est toujours exigé
 * (même s'il n'y en a qu'un) — le tour se suspend dessus
 * ({@link ActivationCursor#pendingPromotions()}), avant même la pose d'impacts
 * en attente pour que ses propres gains s'y ajoutent.
 *
 * <p>Deux autres gains suspendent le tour sur un choix — {@link ChoisirAttribut} —
 * plutôt qu'un emplacement d'action ou un jeton : {@code anyAttribute} (choix
 * libre parmi les 4 pistes, toujours ambigu) et {@code lowestAttribute} (la
 * piste la plus basse, résolue <strong>sans</strong> suspension si elle est
 * seule à ce niveau — {@link GainResolver} l'avance directement — sinon un choix
 * entre les pistes à égalité). Même priorité que la Promotion : avant la pose
 * d'impacts, pour que le cran gagné s'y ajoute au besoin.
 *
 * <p>Résoudre un bonus d'arrivée peut relancer une <strong>cascade</strong> :
 * les submersibles gagnés rejoignent le stock du joueur, et chaque pion impact,
 * promotion ou choix d'attribut gagné doit être résolu ({@link PoserImpact}/
 * {@link Promouvoir}/{@link ChoisirAttribut}) avant de poursuivre le tour — le
 * même mécanisme qu'en préparation, réutilisé via {@link ImpactPlacement}.
 *
 * <p>Le contexte de tour — spécialiste activé, avancement dans sa chaîne,
 * impacts, promotions et choix d'attribut en attente — vit dans
 * l'{@link ActivationCursor}, copié avec l'état (déc. 2 et 3).
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
        if (mustChoosePromotion(state)) {
            return promotionChoices(state, currentPlayer(state));
        }
        if (mustChooseAnyAttribute(state)) {
            return anyAttributeChoices();
        }
        if (mustChooseLowestAttribute(state)) {
            return lowestAttributeChoices(state, currentPlayer(state));
        }
        if (mustPlaceImpact(state)) {
            return impactPlacements(state);
        }
        ActivationCursor cursor = state.activationCursor();
        int player = currentPlayer(state);
        if (cursor.resolvingTokenAction()) {
            return tokenActionMoves(state, player, cursor);
        }
        List<Action> actions = new ArrayList<>();
        boolean canEndTurn = state.player(player).heldDiveTokens().size() <= 1;
        if (cursor.activatedThisTurn()) {
            if (currentSlotOffers(state, player, cursor, ActionType.TRAVEL)) {
                actions.addAll(voyagesOf(state, player));
            }
            if (currentSlotOffers(state, player, cursor, ActionType.SONAR)) {
                actions.addAll(sonarsOf(state, player));
            }
            if (currentSlotOffers(state, player, cursor, ActionType.DIVE)) {
                actions.addAll(divesOf(state, player));
            }
            if (currentSlotOffers(state, player, cursor, ActionType.CONSERVE)) {
                actions.addAll(conservationsOf(state, player));
            }
            if (currentSlotOffers(state, player, cursor, ActionType.PUBLISH)) {
                actions.addAll(publierOf(state, player));
            }
            if (currentSlotOffers(state, player, cursor, ActionType.PROMOTE)) {
                actions.addAll(promotionChoices(state, player));
            }
            actions.addAll(spendTokenChoices(state, player));
            if (canEndTurn) {
                actions.add(new TerminerTour());
                actions.add(new Passer());
            }
        } else {
            actions.addAll(activationsOf(state, player));
            actions.addAll(spendTokenChoices(state, player));
            if (canEndTurn) {
                actions.add(new Passer());
            }
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
        if (mustChoosePromotion(state)) {
            placePendingPromotion(state, player, cursor, action);
            return;
        }
        if (mustChooseAnyAttribute(state)) {
            placePendingAnyAttribute(state, player, cursor, action);
            return;
        }
        if (mustChooseLowestAttribute(state)) {
            placePendingLowestAttribute(state, player, cursor, action);
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
                        cursor.turnPosition(), cursor.passed(), activer.specialistId(), 0, 0, 0, 0, 0, null, null));
            }
            case Voyager voyager -> applyVoyager(state, player, cursor, voyager);
            case Sonar sonar -> applySonar(state, player, cursor, sonar);
            case Dive dive -> applyDive(state, player, cursor, dive);
            case Conserver conserver -> applyConserver(state, player, cursor, conserver);
            case Publier publier -> applyPublier(state, player, cursor, publier);
            case Promouvoir promouvoir -> applyPromouvoir(state, player, cursor, promouvoir);
            case DepenserJeton depenser -> applyDepenserJeton(state, player, cursor, depenser);
            case TerminerTour ignored -> {
                if (!cursor.activatedThisTurn()) {
                    throw new IllegalStateException("Rien à terminer : agir ou passer");
                }
                requireAtMostOneHeldToken(state, player);
                state.setActivationCursor(new ActivationCursor(nextActivePosition(state, cursor, cursor.passed()),
                        cursor.passed(), null, 0, 0, 0, 0, 0, null, null));
            }
            case Passer ignored -> {
                requireAtMostOneHeldToken(state, player);
                Set<Integer> passed = new HashSet<>(cursor.passed());
                passed.add(player);
                state.setActivationCursor(new ActivationCursor(
                        nextActivePosition(state, cursor, passed), passed, null, 0, 0, 0, 0, 0, null, null));
            }
            default -> throw new IllegalStateException("Coup inattendu en activation : " + action);
        }
    }

    /**
     * Applique un Voyage : déplace le submersible puis encaisse le bonus d'arrivée.
     * Hors chaîne du spécialiste quand il vient d'une dépense de jeton
     * ({@code pendingTokenAction} déjà réglé sur {@code TRAVEL}) — sinon exige
     * l'activation et l'emplacement courant.
     */
    private static void applyVoyager(GameState state, int player, ActivationCursor cursor, Voyager voyager) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.TRAVEL;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Voyage sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.TRAVEL);
        }
        requireReachable(state, player, voyager);
        state.oceanBoard().moveVessel(voyager.from(), voyager.to(), player);
        EffectOutcome arrival = resolveArrival(state, player, voyager.to());
        state.setActivationCursor(viaToken
                ? cursorAfterOffChainResolution(cursor, arrival)
                : cursorAfterChainStep(cursor, arrival));
    }

    /**
     * Applique un Sonar : dépenser un disque de transit, le poser sur la case libre
     * la plus à gauche de la piste, puis résoudre cette case. Une <strong>récompense</strong>
     * encaisse ses gains ; une <strong>découverte</strong> tire deux tuiles et suspend le
     * tour sur le choix de celle à garder (le reste de la découverte s'enchaîne ensuite).
     * Hors chaîne du spécialiste quand il vient d'une dépense de jeton.
     */
    private static void applySonar(GameState state, int player, ActivationCursor cursor, Sonar sonar) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.SONAR;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Sonar sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.SONAR);
        }
        SonarSpot spot = requireSonarSpot(state, player, sonar);
        if (exhaustedDiscovery(state, spot)) {
            throw new IllegalStateException("Pioche épuisée : plus aucune tuile à découvrir (règle limite à venir)");
        }
        state.player(player).spendTransitDisc();
        state.oceanBoard().placeSonarDisc(sonar.cell(), sonar.trackIndex(), player);
        switch (spot) {
            case SonarSpot.Reward reward -> {
                EffectOutcome outcome = resolveAndCollect(state, player, reward.gains());
                state.setActivationCursor(viaToken
                        ? cursorAfterOffChainResolution(cursor, outcome)
                        : cursorAfterChainStep(cursor, outcome));
            }
            case SonarSpot.Discover discover -> {
                Set<Integer> levels = drawableLevels(state, discover);
                if (levels.isEmpty()) {
                    // aire de jeu pleine : pas de tuile à piocher, 1 impact à la place
                    EffectOutcome outcome = resolveAndCollect(state, player, List.of(Gain.IMPACT));
                    state.setActivationCursor(viaToken
                            ? cursorAfterOffChainResolution(cursor, outcome)
                            : cursorAfterChainStep(cursor, outcome));
                    return;
                }
                List<String> drawn = state.discoveryPile().draw(DISCOVERY_DRAW, levels, state.random());
                state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                        cursor.activatedSpecialist(), cursor.actionStep(), 0, 0, 0, 0,
                        PendingDiscovery.toChooseFrom(drawn), null));
            }
        }
    }

    /**
     * Applique une Plongée : prend le jeton du sommet du site visé et l'ajoute à la
     * main du joueur, non résolu. Hors chaîne du spécialiste quand elle vient d'une
     * dépense de jeton — sinon exige l'activation et l'emplacement courant.
     */
    private static void applyDive(GameState state, int player, ActivationCursor cursor, Dive dive) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.DIVE;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Plongée sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.DIVE);
        }
        requireDiveSite(state, player, dive);
        String tokenId = state.oceanBoard().takeDiveToken(dive.cell(), dive.siteId());
        state.player(player).receiveDiveToken(tokenId);
        state.setActivationCursor(viaToken
                ? cursorAfterOffChainResolution(cursor, EffectOutcome.NONE)
                : cursorAfterChainStep(cursor, EffectOutcome.NONE));
    }

    /**
     * Applique une Conservation : paie le coût en recherche du site visé, y pose
     * un disque de transit, puis encaisse ses gains. Hors chaîne du spécialiste
     * quand elle vient d'une dépense de jeton — sinon exige l'activation et
     * l'emplacement courant.
     */
    private static void applyConserver(GameState state, int player, ActivationCursor cursor, Conserver move) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.CONSERVE;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Conservation sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.CONSERVE);
        }
        ConservationSite site = requireConservationSite(state, player, move);
        state.player(player).spendResearch(site.cost());
        state.player(player).spendTransitDisc();
        state.oceanBoard().placeConservationDisc(move.cell(), move.siteId(), player);
        EffectOutcome outcome = resolveAndCollect(state, player, site.gains());
        state.setActivationCursor(viaToken
                ? cursorAfterOffChainResolution(cursor, outcome)
                : cursorAfterChainStep(cursor, outcome));
    }

    /**
     * Applique une Publication : paie le coût en recherche de la revue à l'étude
     * visée, y pose un disque de transit sur le site correspondant, encaisse les
     * gains propres du site, ceux de la revue pour le joueur actif et ceux
     * réservés aux adversaires, puis retire la revue du marché (réassort depuis
     * la pioche). Hors chaîne du spécialiste quand elle vient d'une dépense de
     * jeton — sinon exige l'activation et l'emplacement courant.
     */
    private static void applyPublier(GameState state, int player, ActivationCursor cursor, Publier move) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.PUBLISH;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Publication sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.PUBLISH);
        }
        Journal journal = requirePublishableJournal(state, player, move);
        JournalSite site = requireJournalSite(state, player, journal, move);
        state.player(player).spendResearch(journal.researchCost());
        state.player(player).spendTransitDisc();
        state.oceanBoard().placeJournalDisc(move.cell(), move.siteId(), player);
        EffectOutcome siteOutcome = resolveAndCollect(state, player, site.gains());
        EffectOutcome publisherOutcome = resolveAndCollect(state, player, journal.publisherGains());
        EffectOutcome outcome = siteOutcome.plus(publisherOutcome);
        for (int other = 0; other < state.playerCount(); other++) {
            if (other == player) {
                continue;
            }
            EffectOutcome opponentOutcome = GainResolver.resolve(state.player(other), journal.opponentsGains());
            state.player(other).gainVessels(opponentOutcome.vesselsEarned());
            if (opponentOutcome.impactsEarned() > 0) {
                throw new IllegalStateException(
                        "Impact gagné par un adversaire via Publication : pose non prise en charge");
            }
            if (opponentOutcome.promotionsEarned() > 0) {
                throw new IllegalStateException(
                        "Promotion gagnée par un adversaire via Publication : résolution non prise en charge");
            }
            if (opponentOutcome.anyAttributeEarned() > 0 || opponentOutcome.lowestAttributeEarned() > 0) {
                throw new IllegalStateException(
                        "Choix d'attribut ambigu gagné par un adversaire via Publication : résolution non prise "
                                + "en charge");
            }
        }
        state.player(player).acquireJournal(journal.id());
        state.journalMarket().publish(journal.id(), state.random());
        state.setActivationCursor(viaToken
                ? cursorAfterOffChainResolution(cursor, outcome)
                : cursorAfterChainStep(cursor, outcome));
    }

    /**
     * Applique une Promotion choisie comme emplacement d'action dédié d'une chaîne
     * ou accordée par un jeton (par opposition à un gain {@code promote} résolu au
     * passage — {@link #placePendingPromotion}) : retourne la tuile Junior visée
     * côté Senior et encaisse ses gains immédiats. Hors chaîne du spécialiste quand
     * elle vient d'une dépense de jeton — sinon exige l'activation et l'emplacement
     * courant.
     */
    private static void applyPromouvoir(GameState state, int player, ActivationCursor cursor, Promouvoir move) {
        boolean viaToken = cursor.pendingTokenAction() == ActionType.PROMOTE;
        if (!viaToken) {
            if (!cursor.activatedThisTurn()) {
                throw new IllegalStateException("Promotion sans spécialiste activé");
            }
            requireSlotOffers(state, player, cursor, ActionType.PROMOTE);
        }
        requirePromotable(state, player, move.specialistId());
        EffectOutcome outcome = collectPendingDecisions(state, player, applyPromotion(state, player, move.specialistId()));
        state.setActivationCursor(viaToken
                ? cursorAfterOffChainResolution(cursor, outcome)
                : cursorAfterChainStep(cursor, outcome));
    }

    /**
     * Applique une dépense de jeton : résout l'option choisie du jeton en main.
     * Un lot de gains est encaissé entièrement par ce seul coup (cascade
     * d'impacts comprise) ; une action accordée suspend le tour sur cette seule
     * action ({@link Sonar}/{@link Voyager}/{@link Dive} la joueront ensuite).
     */
    private static void applyDepenserJeton(GameState state, int player, ActivationCursor cursor,
                                           DepenserJeton move) {
        if (cursor.resolvingTokenAction()) {
            throw new IllegalStateException("Une action de jeton reste à jouer avant d'en dépenser un autre");
        }
        List<String> held = state.player(player).heldDiveTokens();
        if (move.heldIndex() >= held.size()) {
            throw new IllegalStateException("Aucun jeton en main à cet index : " + move.heldIndex());
        }
        DiveToken token = diveTokenById(state, held.get(move.heldIndex()));
        if (move.optionIndex() >= token.options().size()) {
            throw new IllegalStateException(
                    "Option inexistante sur " + token.id() + " : " + move.optionIndex());
        }
        DiveOption option = token.options().get(move.optionIndex());
        if (!isPlayable(state, player, option)) {
            throw new IllegalStateException("Option non jouable pour l'instant : " + option);
        }
        state.player(player).resolveDiveToken(move.heldIndex());
        switch (option) {
            case DiveOption.Gains gains -> {
                payCost(state.player(player), gains.cost());
                EffectOutcome outcome = resolveAndCollect(state, player, gains.gains());
                state.setActivationCursor(
                        cursorAfterOffChainResolution(cursor, outcome));
            }
            case DiveOption.TriggersAction trigger -> state.setActivationCursor(new ActivationCursor(
                    cursor.turnPosition(), cursor.passed(), cursor.activatedSpecialist(),
                    cursor.actionStep(), cursor.pendingImpacts(), cursor.pendingPromotions(),
                    cursor.pendingAnyAttribute(), cursor.pendingLowestAttribute(), null, trigger.type()));
        }
    }

    /** Paie un coût d'option (vocabulaire des gains) — seul le disque de réserve est pris en charge. */
    private static void payCost(Player player, List<Gain> cost) {
        for (Gain gain : cost) {
            if (gain != Gain.DISC) {
                throw new IllegalStateException("Coût d'option de jeton non pris en charge : " + gain.code());
            }
            player.spendReserveDisc();
        }
    }

    /** Le curseur après une action résolue en entier au fil de la chaîne : avance l'emplacement courant. */
    private static ActivationCursor cursorAfterChainStep(ActivationCursor cursor, EffectOutcome outcome) {
        return new ActivationCursor(cursor.turnPosition(), cursor.passed(), cursor.activatedSpecialist(),
                cursor.actionStep() + 1, outcome.impactsEarned(), outcome.promotionsEarned(),
                outcome.anyAttributeEarned(), outcome.lowestAttributeEarned(), null, null);
    }

    /**
     * Le curseur après une action résolue <strong>hors chaîne</strong> — accordée par
     * un jeton, ou un lot de gains d'une option de jeton — sans avancer l'emplacement
     * courant de la chaîne du spécialiste.
     */
    private static ActivationCursor cursorAfterOffChainResolution(ActivationCursor cursor, EffectOutcome outcome) {
        return new ActivationCursor(cursor.turnPosition(), cursor.passed(), cursor.activatedSpecialist(),
                cursor.actionStep(), outcome.impactsEarned(), outcome.promotionsEarned(),
                outcome.anyAttributeEarned(), outcome.lowestAttributeEarned(), null, null);
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
        EffectOutcome outcome = collectPendingDecisions(state, player,
                ImpactPlacement.place(state.missionBoard(), state.player(player), hex, player));
        int stillPending = cursor.pendingImpacts() - 1 + outcome.impactsEarned();
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep(), stillPending, outcome.promotionsEarned(),
                outcome.anyAttributeEarned(), outcome.lowestAttributeEarned(), null, null));
    }

    /**
     * Vrai s'il reste une promotion à résoudre (le tour est suspendu) — vérifié
     * avant la pose d'impacts pour que les gains propres d'une promotion (senior
     * fraîchement promu) s'y ajoutent. {@link #collectPendingDecisions} garantit qu'un
     * compte en attente n'est jamais positif sans au moins un Junior promouvable :
     * pas besoin de le revérifier ici.
     */
    private static boolean mustChoosePromotion(GameState state) {
        return state.activationCursor().pendingPromotions() > 0;
    }

    private static List<Action> promotionChoices(GameState state, int playerIndex) {
        return promotableJuniors(state.player(playerIndex)).stream()
                .map(held -> (Action) new Promouvoir(held.specialist().id()))
                .toList();
    }

    /**
     * Résout un gain {@code promote} en attente choisi par {@link Promouvoir} :
     * promeut la tuile visée, encaisse ses gains immédiats, et rend la main sur le
     * reste du lot d'origine (moins celui-ci) combiné aux gains de la promotion —
     * l'ensemble repassé au filtre « aucun Junior restant » de {@link
     * #collectPendingDecisions}.
     */
    private static void placePendingPromotion(GameState state, int player, ActivationCursor cursor, Action action) {
        if (!(action instanceof Promouvoir move)) {
            throw new IllegalStateException("Une promotion reste à choisir avant de poursuivre le tour");
        }
        requirePromotable(state, player, move.specialistId());
        EffectOutcome raw = applyPromotion(state, player, move.specialistId());
        int totalPromotions = cursor.pendingPromotions() - 1 + raw.promotionsEarned();
        EffectOutcome collected = collectPendingDecisions(state, player,
                new EffectOutcome(raw.impactsEarned(), raw.vesselsEarned(), totalPromotions,
                        raw.anyAttributeEarned(), raw.lowestAttributeEarned()));
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep(), cursor.pendingImpacts() + collected.impactsEarned(),
                collected.promotionsEarned(), cursor.pendingAnyAttribute() + collected.anyAttributeEarned(),
                cursor.pendingLowestAttribute() + collected.lowestAttributeEarned(), null, null));
    }

    /**
     * Résout un lot de gains et encaisse aussitôt ses submersibles et promotions
     * triviales — via {@link #collectPendingDecisions}.
     */
    private static EffectOutcome resolveAndCollect(GameState state, int player, List<Gain> gains) {
        return collectPendingDecisions(state, player, GainResolver.resolve(state.player(player), gains));
    }

    /**
     * Encaisse les submersibles d'une résolution de gains et filtre ses promotions :
     * si le joueur ne détient alors aucun Junior promouvable, elles sont
     * silencieusement perdues ; sinon elles restent en attente d'un coup
     * {@link Promouvoir} explicite, même s'il n'y en a qu'un seul possible — jamais
     * appliqué à la place du joueur. Les choix d'attribut ({@code anyAttribute},
     * {@code lowestAttribute} ambigu) traversent tels quels : ils n'ont pas de cas
     * « perdu », un choix reste toujours possible entre au moins deux pistes.
     */
    private static EffectOutcome collectPendingDecisions(GameState state, int player, EffectOutcome outcome) {
        state.player(player).gainVessels(outcome.vesselsEarned());
        boolean resolvable = !promotableJuniors(state.player(player)).isEmpty();
        int promotions = resolvable ? outcome.promotionsEarned() : 0;
        return new EffectOutcome(outcome.impactsEarned(), 0, promotions, outcome.anyAttributeEarned(),
                outcome.lowestAttributeEarned());
    }

    /** Retourne le Junior visé côté Senior et renvoie ses gains immédiats, non encore encaissés. */
    private static EffectOutcome applyPromotion(GameState state, int player, String specialistId) {
        HeldSpecialist promoted = state.player(player).promote(specialistId);
        return GainResolver.resolve(state.player(player), promoted.activeSide().immediateGains());
    }

    /** Les tuiles Junior détenues promouvables — jamais le chef d'équipe, qui n'a pas de face Senior distincte. */
    private static List<HeldSpecialist> promotableJuniors(Player player) {
        return player.specialists().stream()
                .filter(held -> held.face() == SpecialistFace.JUNIOR && !held.specialist().teamLeader())
                .toList();
    }

    private static void requirePromotable(GameState state, int player, String specialistId) {
        boolean promotable = promotableJuniors(state.player(player)).stream()
                .anyMatch(held -> held.specialist().id().equals(specialistId));
        if (!promotable) {
            throw new IllegalStateException("Tuile non promouvable : " + specialistId);
        }
    }

    /**
     * Vrai s'il reste un choix d'attribut libre à résoudre (le tour est suspendu) —
     * toujours ambigu, les 4 pistes sont toujours valides.
     */
    private static boolean mustChooseAnyAttribute(GameState state) {
        return state.activationCursor().pendingAnyAttribute() > 0;
    }

    /** Les 4 pistes, toujours valides pour un choix {@code anyAttribute}. */
    private static List<Action> anyAttributeChoices() {
        return List.of(Attribute.values()).stream()
                .map(attribute -> (Action) new ChoisirAttribut(attribute))
                .toList();
    }

    private static void placePendingAnyAttribute(GameState state, int player, ActivationCursor cursor,
                                                  Action action) {
        if (!(action instanceof ChoisirAttribut choice)) {
            throw new IllegalStateException("Un choix d'attribut reste à faire avant de poursuivre le tour");
        }
        EffectOutcome outcome = resolveAttributeChoice(state, player, choice.attribute());
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep(),
                cursor.pendingImpacts() + outcome.impactsEarned(),
                cursor.pendingPromotions() + outcome.promotionsEarned(),
                cursor.pendingAnyAttribute() - 1 + outcome.anyAttributeEarned(),
                cursor.pendingLowestAttribute() + outcome.lowestAttributeEarned(), null, null));
    }

    /**
     * Vrai s'il reste un choix {@code lowestAttribute} ambigu à résoudre (le tour
     * est suspendu) — ne vaut jamais plus de 0 sans égalité entre pistes au plus
     * bas niveau, {@link GainResolver} ayant déjà résolu le cas non ambigu.
     */
    private static boolean mustChooseLowestAttribute(GameState state) {
        return state.activationCursor().pendingLowestAttribute() > 0;
    }

    private static List<Action> lowestAttributeChoices(GameState state, int playerIndex) {
        return GainResolver.lowestAttributes(state.player(playerIndex)).stream()
                .map(attribute -> (Action) new ChoisirAttribut(attribute))
                .toList();
    }

    private static void placePendingLowestAttribute(GameState state, int player, ActivationCursor cursor,
                                                     Action action) {
        if (!(action instanceof ChoisirAttribut choice)) {
            throw new IllegalStateException("Un choix d'attribut reste à faire avant de poursuivre le tour");
        }
        if (!GainResolver.lowestAttributes(state.player(player)).contains(choice.attribute())) {
            throw new IllegalStateException("Piste hors égalité au plus bas niveau : " + choice.attribute());
        }
        EffectOutcome outcome = resolveAttributeChoice(state, player, choice.attribute());
        state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                cursor.activatedSpecialist(), cursor.actionStep(),
                cursor.pendingImpacts() + outcome.impactsEarned(),
                cursor.pendingPromotions() + outcome.promotionsEarned(),
                cursor.pendingAnyAttribute() + outcome.anyAttributeEarned(),
                cursor.pendingLowestAttribute() - 1 + outcome.lowestAttributeEarned(), null, null));
    }

    /** Avance la piste choisie d'un cran — même résolveur qu'un gain fixe, même cascade. */
    private static EffectOutcome resolveAttributeChoice(GameState state, int player, Attribute attribute) {
        return resolveAndCollect(state, player, List.of(Gain.fromCode(attribute.code())));
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
            state.setActivationCursor(new ActivationCursor(cursor.turnPosition(), cursor.passed(),
                    cursor.activatedSpecialist(), cursor.actionStep(), 0, 0, 0, 0, pending.kept(keep.tileId()), null));
            return;
        }
        if (!(action instanceof PoserTuile placement)) {
            throw new IllegalStateException("La tuile de découverte reste à poser");
        }
        requireValidPlacement(state, pending.keptTile(), placement.cell());
        state.oceanBoard().discoverTile(placement.cell(), pending.keptTile(), player);
        DiveSiteSetup.stack(state.oceanBoard(), state.diveTokenPile(), state.random(),
                placement.cell(), tileById(state, pending.keptTile()));
        EffectOutcome outcome = resolveDiscoverBonus(state, player, pending.keptTile());
        state.setActivationCursor(cursorAfterChainStep(cursor, outcome));
    }

    /**
     * Les cases où poser une tuile de découverte : une case libre de la profondeur
     * de la tuile, et — hors niveau 1 — sous une zone déjà en jeu (la case juste
     * au-dessus est occupée).
     */
    private static List<Cell> validPlacements(GameState state, String tileId) {
        return validPlacementsAtDepth(state, tileById(state, tileId).depth());
    }

    private static List<Cell> validPlacementsAtDepth(GameState state, int depth) {
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
        return resolveAndCollect(state, player, tile.discoverBonus());
    }

    /**
     * Encaisse le bonus d'arrivée de la zone de destination : applique ses gains
     * directs, verse les submersibles gagnés au stock, et renvoie les impacts et
     * promotions gagnés (à mettre en jeu par la cascade).
     */
    private static EffectOutcome resolveArrival(GameState state, int player, Cell destination) {
        OceanTile tile = tileAt(state, destination);
        return resolveAndCollect(state, player, tile.arrivalBonus());
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

    /** Le jeton de plongée d'identifiant donné dans le catalogue. */
    private static DiveToken diveTokenById(GameState state, String tokenId) {
        return state.diveTokenCatalog().byId(tokenId).orElseThrow(
                () -> new IllegalStateException("Jeton de plongée inconnu au catalogue : " + tokenId));
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
     * Les Voyages légaux : pour chaque zone où le joueur a un submersible, chaque
     * destination accessible à son niveau de technologie. Ne juge pas si l'action est
     * offerte (chaîne du spécialiste ou jeton) — c'est l'affaire de l'appelant.
     */
    private static List<Voyager> voyagesOf(GameState state, int playerIndex) {
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
     * Les Sonars légaux s'il reste un disque en transit à poser : pour chaque zone où
     * le joueur a un submersible, chaque piste qui a encore une case libre —
     * récompense (gains immédiats) ou découverte (piocher et poser une tuile). Ne
     * juge pas si l'action est offerte — c'est l'affaire de l'appelant.
     */
    private static List<Sonar> sonarsOf(GameState state, int playerIndex) {
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

    /**
     * Les Plongées légales, sans coût : pour chaque zone où le joueur a un
     * submersible, chaque site de plongée qui a encore au moins un jeton empilé. Ne
     * juge pas si l'action est offerte — c'est l'affaire de l'appelant.
     */
    private static List<Dive> divesOf(GameState state, int playerIndex) {
        OceanBoard ocean = state.oceanBoard();
        List<Dive> dives = new ArrayList<>();
        for (Cell cell : ocean.vesselCells(playerIndex)) {
            for (DiveSite site : tileAt(state, cell).diveSites()) {
                if (ocean.diveTokenCount(cell, site.id()) > 0) {
                    dives.add(new Dive(cell, site.id()));
                }
            }
        }
        return dives;
    }

    /**
     * Les Conservations légales s'il reste un disque en transit à poser : pour
     * chaque zone où le joueur a un submersible, chaque site de conservation encore
     * libre dont le coût en recherche est payable. Ne juge pas si l'action est
     * offerte — c'est l'affaire de l'appelant.
     */
    private static List<Conserver> conservationsOf(GameState state, int playerIndex) {
        if (state.player(playerIndex).transitDiscs() == 0) {
            return List.of();
        }
        OceanBoard ocean = state.oceanBoard();
        int research = state.player(playerIndex).research();
        List<Conserver> conservations = new ArrayList<>();
        for (Cell cell : ocean.vesselCells(playerIndex)) {
            for (ConservationSite site : tileAt(state, cell).conservationSites()) {
                if (!ocean.conservationSiteOccupied(cell, site.id()) && research >= site.cost()) {
                    conservations.add(new Conserver(cell, site.id()));
                }
            }
        }
        return conservations;
    }

    /**
     * Les Publications légales s'il reste un disque en transit à poser : pour
     * chaque revue à l'étude dont le coût est payable, chaque zone où le joueur
     * a un submersible, chaque site de publication encore libre dont le symbole
     * de domaine y correspond. Ne juge pas si l'action est offerte — c'est
     * l'affaire de l'appelant.
     */
    private static List<Publier> publierOf(GameState state, int playerIndex) {
        if (state.player(playerIndex).transitDiscs() == 0) {
            return List.of();
        }
        OceanBoard ocean = state.oceanBoard();
        int research = state.player(playerIndex).research();
        List<Publier> publications = new ArrayList<>();
        for (String journalId : state.journalMarket().underStudy()) {
            Journal journal = journalById(state, journalId);
            if (research < journal.researchCost()) {
                continue;
            }
            for (Cell cell : ocean.vesselCells(playerIndex)) {
                for (JournalSite site : tileAt(state, cell).journalSites()) {
                    if (!ocean.journalSiteOccupied(cell, site.id())
                            && journal.fieldSymbols().contains(site.fieldSymbol())) {
                        publications.add(new Publier(journalId, cell, site.id()));
                    }
                }
            }
        }
        return publications;
    }

    /**
     * Vérifie la légalité d'une Publication et renvoie la revue visée : à
     * l'étude au marché, et coût payable.
     *
     * @throws IllegalStateException si l'une de ces conditions n'est pas remplie
     */
    private static Journal requirePublishableJournal(GameState state, int playerIndex, Publier move) {
        if (!state.journalMarket().underStudy().contains(move.journalId())) {
            throw new IllegalStateException("Revue absente du marché à l'étude : " + move.journalId());
        }
        Journal journal = journalById(state, move.journalId());
        if (state.player(playerIndex).research() < journal.researchCost()) {
            throw new IllegalStateException("Recherche insuffisante pour la publication : " + journal.researchCost());
        }
        return journal;
    }

    /**
     * Vérifie la légalité du site visé par une Publication et le renvoie :
     * présence d'un submersible, existence du site, site encore libre, et
     * symbole de domaine correspondant à la revue.
     *
     * @throws IllegalStateException si l'une de ces conditions n'est pas remplie
     */
    private static JournalSite requireJournalSite(GameState state, int playerIndex, Journal journal, Publier move) {
        if (state.oceanBoard().vesselCount(move.cell(), playerIndex) == 0) {
            throw new IllegalStateException("Aucun submersible dans la zone de la Publication : " + move.cell());
        }
        JournalSite site = tileAt(state, move.cell()).journalSites().stream()
                .filter(candidate -> candidate.id().equals(move.siteId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Site de publication inexistant en " + move.cell() + " : " + move.siteId()));
        if (state.oceanBoard().journalSiteOccupied(move.cell(), move.siteId())) {
            throw new IllegalStateException(
                    "Site de publication déjà occupé : " + move.cell() + "/" + move.siteId());
        }
        if (!journal.fieldSymbols().contains(site.fieldSymbol())) {
            throw new IllegalStateException(
                    "Symbole de domaine du site incompatible avec la revue " + journal.id() + " : "
                            + site.fieldSymbol());
        }
        return site;
    }

    /** La revue d'identifiant donné dans le catalogue. */
    private static Journal journalById(GameState state, String journalId) {
        return state.journalCatalog().byId(journalId).orElseThrow(
                () -> new IllegalStateException("Revue inconnue au catalogue : " + journalId));
    }

    /**
     * Les dépenses de jeton légales : pour chaque jeton en main, chacune de ses
     * options actuellement réalisable.
     */
    private static List<DepenserJeton> spendTokenChoices(GameState state, int playerIndex) {
        List<String> held = state.player(playerIndex).heldDiveTokens();
        List<DepenserJeton> choices = new ArrayList<>();
        for (int heldIndex = 0; heldIndex < held.size(); heldIndex++) {
            List<DiveOption> options = diveTokenById(state, held.get(heldIndex)).options();
            for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                if (isPlayable(state, playerIndex, options.get(optionIndex))) {
                    choices.add(new DepenserJeton(heldIndex, optionIndex));
                }
            }
        }
        return choices;
    }

    /**
     * Vrai si l'option est réalisable maintenant : un lot de gains dont le coût est
     * payable (tout gain est désormais résolvable, au besoin via une décision
     * suspendue), ou une action déjà jouable par le moteur (Sonar, Voyage,
     * Plongée, Conservation, Publication, Promotion) qui a au moins un coup
     * possible.
     */
    private static boolean isPlayable(GameState state, int playerIndex, DiveOption option) {
        return switch (option) {
            case DiveOption.Gains gains -> affordable(state, playerIndex, gains.cost());
            case DiveOption.TriggersAction trigger -> switch (trigger.type()) {
                case SONAR -> !sonarsOf(state, playerIndex).isEmpty();
                case TRAVEL -> !voyagesOf(state, playerIndex).isEmpty();
                case DIVE -> !divesOf(state, playerIndex).isEmpty();
                case CONSERVE -> !conservationsOf(state, playerIndex).isEmpty();
                case PUBLISH -> !publierOf(state, playerIndex).isEmpty();
                case PROMOTE -> !promotionChoices(state, playerIndex).isEmpty();
            };
        };
    }

    /** Vrai si le joueur peut payer ce coût — seul le disque de réserve est pris en charge. */
    private static boolean affordable(GameState state, int playerIndex, List<Gain> cost) {
        int discsCost = 0;
        for (Gain gain : cost) {
            if (gain != Gain.DISC) {
                throw new IllegalStateException("Coût d'option de jeton non pris en charge : " + gain.code());
            }
            discsCost++;
        }
        return state.player(playerIndex).reserveDiscs() >= discsCost;
    }

    /** Vrai si la case est une découverte et que la pioche n'a plus aucune tuile, à aucun niveau. */
    private static boolean exhaustedDiscovery(GameState state, SonarSpot spot) {
        return spot instanceof SonarSpot.Discover && state.discoveryPile().size() == 0;
    }

    /**
     * Les niveaux où piocher pour cette découverte : ceux demandés qui ont à la fois une
     * tuile à piocher et une case où la poser. Si le niveau demandé est saturé, on se
     * rabat sur le moins profond des niveaux encore utilisables. Vide quand plus aucune
     * tuile ne peut être posée nulle part : l'aire de jeu est pleine.
     */
    private static Set<Integer> drawableLevels(GameState state, SonarSpot.Discover discover) {
        TreeSet<Integer> placeable = new TreeSet<>();
        for (int depth : state.discoveryPile().availableDepths()) {
            if (!validPlacementsAtDepth(state, depth).isEmpty()) {
                placeable.add(depth);
            }
        }
        Set<Integer> requested = new TreeSet<>(placeable);
        requested.retainAll(discover.levels());
        if (requested.isEmpty() && !placeable.isEmpty()) {
            return Set.of(placeable.first());
        }
        return requested;
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

    /**
     * Vérifie la légalité d'une Plongée : présence d'un submersible, existence du
     * site, et au moins un jeton encore empilé.
     *
     * @throws IllegalStateException si l'une de ces conditions n'est pas remplie
     */
    private static void requireDiveSite(GameState state, int playerIndex, Dive dive) {
        if (state.oceanBoard().vesselCount(dive.cell(), playerIndex) == 0) {
            throw new IllegalStateException("Aucun submersible dans la zone de la Plongée : " + dive.cell());
        }
        boolean known = tileAt(state, dive.cell()).diveSites().stream()
                .anyMatch(site -> site.id().equals(dive.siteId()));
        if (!known) {
            throw new IllegalStateException(
                    "Site de plongée inexistant en " + dive.cell() + " : " + dive.siteId());
        }
        if (state.oceanBoard().diveTokenCount(dive.cell(), dive.siteId()) == 0) {
            throw new IllegalStateException(
                    "Site de plongée déjà vide : " + dive.cell() + "/" + dive.siteId());
        }
    }

    /**
     * Vérifie la légalité d'une Conservation et renvoie le site visé : présence
     * d'un submersible, existence du site, site encore libre, et recherche
     * suffisante pour payer son coût.
     *
     * @throws IllegalStateException si l'une de ces conditions n'est pas remplie
     */
    private static ConservationSite requireConservationSite(GameState state, int playerIndex, Conserver move) {
        if (state.oceanBoard().vesselCount(move.cell(), playerIndex) == 0) {
            throw new IllegalStateException("Aucun submersible dans la zone de la Conservation : " + move.cell());
        }
        ConservationSite site = tileAt(state, move.cell()).conservationSites().stream()
                .filter(candidate -> candidate.id().equals(move.siteId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Site de conservation inexistant en " + move.cell() + " : " + move.siteId()));
        if (state.oceanBoard().conservationSiteOccupied(move.cell(), move.siteId())) {
            throw new IllegalStateException(
                    "Site de conservation déjà occupé : " + move.cell() + "/" + move.siteId());
        }
        if (state.player(playerIndex).research() < site.cost()) {
            throw new IllegalStateException("Recherche insuffisante pour la conservation : " + site.cost());
        }
        return site;
    }

    /**
     * Au plus un jeton de plongée peut rester en main à la fin du tour.
     *
     * @throws IllegalStateException si le joueur en détient plus d'un
     */
    private static void requireAtMostOneHeldToken(GameState state, int playerIndex) {
        if (state.player(playerIndex).heldDiveTokens().size() > 1) {
            throw new IllegalStateException(
                    "Au plus un jeton de plongée peut être conservé en fin de tour");
        }
    }

    /** Les coups légaux pendant qu'une action accordée par un jeton reste à jouer. */
    private static List<Action> tokenActionMoves(GameState state, int playerIndex, ActivationCursor cursor) {
        List<Action> actions = new ArrayList<>();
        switch (cursor.pendingTokenAction()) {
            case TRAVEL -> actions.addAll(voyagesOf(state, playerIndex));
            case SONAR -> actions.addAll(sonarsOf(state, playerIndex));
            case DIVE -> actions.addAll(divesOf(state, playerIndex));
            case CONSERVE -> actions.addAll(conservationsOf(state, playerIndex));
            case PUBLISH -> actions.addAll(publierOf(state, playerIndex));
            case PROMOTE -> actions.addAll(promotionChoices(state, playerIndex));
            default -> throw new IllegalStateException(
                    "Action de jeton inattendue : " + cursor.pendingTokenAction());
        }
        return actions;
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
