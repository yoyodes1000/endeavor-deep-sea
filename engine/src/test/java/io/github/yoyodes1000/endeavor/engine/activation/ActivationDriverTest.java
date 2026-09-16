package io.github.yoyodes1000.endeavor.engine.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Conserver;
import io.github.yoyodes1000.endeavor.engine.action.DepenserJeton;
import io.github.yoyodes1000.endeavor.engine.action.Dive;
import io.github.yoyodes1000.endeavor.engine.action.GarderTuile;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.PoserTuile;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.Sonar;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.action.Voyager;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.dive.DiveOption;
import io.github.yoyodes1000.endeavor.engine.dive.DiveToken;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionSlot;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ActivationDriverTest {

    private static GameState game(int players) {
        return GameState.newGame(players, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog());
    }

    @Test
    void auDebutSeulPasserEstLegalEtLaPhaseNEstPasFinie() {
        GameState state = game(3);
        ActivationDriver.begin(state);

        assertFalse(ActivationDriver.isDone(state));
        assertEquals(List.of(new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void laPhaseFinitQuandTousLesJoueursOntPasse() {
        GameState state = game(3);
        ActivationDriver.begin(state);

        ActivationDriver.apply(state, new Passer());
        assertFalse(ActivationDriver.isDone(state));
        ActivationDriver.apply(state, new Passer());
        assertFalse(ActivationDriver.isDone(state));
        ActivationDriver.apply(state, new Passer());

        assertTrue(ActivationDriver.isDone(state));
        assertTrue(ActivationDriver.legalActions(state).isEmpty());
    }

    @Test
    void leRoundRobinSuitLOrdreDuTour() {
        GameState state = game(3);
        state.setFirstPlayerIndex(2); // ordre du tour : [2, 0, 1]
        ActivationDriver.begin(state);

        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2), state.activationCursor().passed(), "le premier joueur passe d'abord");
        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2, 0), state.activationCursor().passed());
        ActivationDriver.apply(state, new Passer());
        assertEquals(java.util.Set.of(2, 0, 1), state.activationCursor().passed());
    }

    @Test
    void passerApresLaFinDePhaseEstRefuse() {
        GameState state = game(1);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Passer());

        assertTrue(ActivationDriver.isDone(state));
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Passer()));
    }

    @Test
    void unCoupInattenduEnActivationEstRefuse() {
        GameState state = game(2);
        ActivationDriver.begin(state);
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Recruter("pilot")));
    }

    @Test
    void activerConsommeUnDisquePuisSeulTerminerOuPasserRestent() {
        GameState state = game(1);
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);

        assertTrue(ActivationDriver.legalActions(state).contains(new Activer("team-leader")));
        ActivationDriver.apply(state, new Activer("team-leader"));

        assertEquals(1, state.player(0).transitDiscs(), "un disque consommé");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void unSeulSpecialistePeutEtreActiveParTour() {
        GameState state = game(1);
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("team-leader"));

        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Activer("team-leader")));
    }

    @Test
    void terminerTourRendLaMainSansSortirDeLaManche() {
        GameState state = game(2);
        state.player(0).moveReserveToTransit(1);
        ActivationDriver.begin(state); // ordre du tour [0, 1], joueur courant 0
        ActivationDriver.apply(state, new Activer("team-leader"));
        ActivationDriver.apply(state, new TerminerTour());

        assertEquals(1, state.activationCursor().turnPosition(), "au joueur suivant");
        assertTrue(state.activationCursor().passed().isEmpty(), "personne n'a quitté la manche");
        assertFalse(ActivationDriver.isDone(state));
    }

    @Test
    void terminerTourSansAvoirAgiEstRefuse() {
        GameState state = game(1);
        ActivationDriver.begin(state);
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new TerminerTour()));
    }

    // --- Voyage -----------------------------------------------------------

    /** Un spécialiste dont la chaîne offre un seul emplacement : le Voyage. */
    private static Specialist travelSpecialist() {
        SpecialistSide junior = new SpecialistSide("Pilot", List.of(),
                List.of(new ActionSlot(List.of(ActionType.TRAVEL))), Optional.empty(), Optional.empty());
        SpecialistSide senior = new SpecialistSide("Pilot S", List.of(), List.of(),
                Optional.empty(), Optional.empty());
        return new Specialist("pilot", OptionalInt.of(1), false, junior, senior);
    }

    /** Joueur 0 en activation, avec un spécialiste voyageur, des disques et un submersible en (1,0). */
    private static GameState readyToTravel() {
        GameState state = game(1);
        state.player(0).recruit(HeldSpecialist.recruited(travelSpecialist()));
        state.player(0).moveReserveToTransit(2);
        state.oceanBoard().addVessels(new Cell(1, 0), 0, 1);
        ActivationDriver.begin(state);
        return state;
    }

    @Test
    void unSpecialisteVoyageurProposeLesVoyagesAccessibles() {
        GameState state = readyToTravel();
        ActivationDriver.apply(state, new Activer("pilot"));

        // niveau de technologie 1 au départ : depuis (1,0), seule (1,1) est accessible
        assertTrue(ActivationDriver.legalActions(state).contains(new Voyager(new Cell(1, 0), new Cell(1, 1))));
    }

    @Test
    void leVoyageDeplaceLeSubmersibleEtConsommeLaChaine() {
        GameState state = readyToTravel();
        ActivationDriver.apply(state, new Activer("pilot"));

        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        assertEquals(0, state.oceanBoard().vesselCount(new Cell(1, 0), 0), "parti du départ");
        assertEquals(1, state.oceanBoard().vesselCount(new Cell(1, 1), 0), "arrivé à destination");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state),
                "chaîne d'un seul emplacement épuisée");
    }

    @Test
    void leVoyageNeDescendPasPlusProfondQueLeNiveau() {
        GameState state = game(1);
        state.player(0).recruit(HeldSpecialist.recruited(travelSpecialist()));
        state.player(0).moveReserveToTransit(2);
        state.oceanBoard().addVessels(new Cell(1, 1), 0, 1);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("pilot"));

        // niveau 1 : la descente en profondeur 2 (2,1) est refusée
        assertFalse(ActivationDriver.legalActions(state).contains(new Voyager(new Cell(1, 1), new Cell(2, 1))));

        // niveau 2 : elle devient possible
        state.player(0).attributes().advance(Attribute.INGENUITY, 2);
        assertTrue(ActivationDriver.legalActions(state).contains(new Voyager(new Cell(1, 1), new Cell(2, 1))));
    }

    @Test
    void sansSubmersibleAucunVoyageNEstPropose() {
        GameState state = game(1);
        state.player(0).recruit(HeldSpecialist.recruited(travelSpecialist()));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("pilot"));

        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void leVoyageAvantActivationEstRefuse() {
        GameState state = readyToTravel();
        assertThrows(IllegalStateException.class,
                () -> ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1))));
    }

    // --- Bonus d'arrivée ---------------------------------------------------

    @Test
    void leVoyageEncaisseLeBonusDArriveeDeLaDestination() {
        GameState state = readyToTravel();
        int before = state.player(0).research();
        ActivationDriver.apply(state, new Activer("pilot"));

        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        assertEquals(before + 1, state.player(0).research(), "le bonus d'arrivée de reef (recherche) est encaissé");
    }

    @Test
    void leBonusDArriveePeutFaireGagnerUnSubmersibleAuStock() {
        GameState state = travelTo(Gain.INGENUITY);
        state.player(0).attributes().advance(Attribute.INGENUITY, 1); // step 1, juste avant la case 2 (submersible)
        ActivationDriver.apply(state, new Activer("pilot"));

        int stockBefore = state.player(0).vesselStock();
        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        assertEquals(stockBefore + 1, state.player(0).vesselStock(), "case 2 d'ingéniosité franchie → submersible au stock");
    }

    @Test
    void leBonusDArriveePeutDeclencherLaCascadeDePoseDImpact() {
        GameState state = travelTo(Gain.IMPACT);
        ActivationDriver.apply(state, new Activer("pilot"));
        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        // impact gagné à l'arrivée : le tour est suspendu sur la pose
        assertEquals(List.of(new PoserImpact(0, 0)), ActivationDriver.legalActions(state));

        ActivationDriver.apply(state, new PoserImpact(0, 0));
        assertTrue(state.missionBoard().isOccupied(state.missionBoard().board().hexAt(0, 0).orElseThrow()));
        // impact posé, chaîne épuisée : le tour reprend, plus qu'à finir ou passer
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    /**
     * Joueur 0 prêt à voyager de (1,0) vers (1,1), dont la tuile accorde {@code arrival}
     * à l'arrivée. Océan et catalogue sur mesure pour éprouver la résolution du bonus.
     */
    private static GameState travelTo(Gain arrival) {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "start");
        ocean.placeTile(new Cell(1, 1), "dest");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("start", "Start", 1, false, List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of()),
                new OceanTile("dest", "Dest", 1, false, List.of(), List.of(arrival), List.of(), List.of(),
                        List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, Fixtures.diveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(travelSpecialist()));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        return state;
    }

    // --- Sonar ------------------------------------------------------------

    private static final ActionSlot SONAR_SLOT = new ActionSlot(List.of(ActionType.SONAR));

    /** Un spécialiste dont la chaîne offre {@code slots} emplacements Sonar. */
    private static Specialist sonarSpecialist(List<ActionSlot> chain) {
        SpecialistSide junior = new SpecialistSide("Sonarist", List.of(), chain, Optional.empty(), Optional.empty());
        SpecialistSide senior = new SpecialistSide("Sonarist S", List.of(), List.of(),
                Optional.empty(), Optional.empty());
        return new Specialist("sonarist", OptionalInt.of(1), false, junior, senior);
    }

    /**
     * Joueur 0 en activation avec un spécialiste Sonar. Deux zones portent un
     * submersible : en (1,0) une piste dont la case libre la plus à gauche est une
     * <strong>récompense</strong> ({@code rewardGains}) suivie d'une découverte ; en
     * (1,1) une piste de <strong>découverte</strong> seule.
     */
    private static GameState readyToSonar(List<Gain> rewardGains, int transitDiscs, List<ActionSlot> chain) {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "reward-tile");
        ocean.placeTile(new Cell(1, 1), "discover-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        ocean.addVessels(new Cell(1, 1), 0, 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("reward-tile", "Reward", 1, false, List.of(), List.of(), List.of(),
                        List.of(new SonarTrack(List.of(
                                new SonarSpot.Reward(rewardGains), new SonarSpot.Discover(List.of(1))))),
                        List.of(), List.of()),
                new OceanTile("discover-tile", "Discover", 1, false, List.of(), List.of(), List.of(),
                        List.of(new SonarTrack(List.of(new SonarSpot.Discover(List.of(1))))), List.of(), List.of()),
                // tuiles non posées : garnissent la pioche pour que les cases découverte soient jouables
                discovered("pile-x", 1, List.of(Gain.RESEARCH)),
                discovered("pile-y", 1, List.of(Gain.RESEARCH))));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, Fixtures.diveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(sonarSpecialist(chain)));
        state.player(0).moveReserveToTransit(transitDiscs);
        ActivationDriver.begin(state);
        return state;
    }

    @Test
    void lesPistesRecompenseEtDecouverteSontToutesDeuxOffertes() {
        GameState state = readyToSonar(List.of(Gain.RESEARCH), 2, List.of(SONAR_SLOT));
        ActivationDriver.apply(state, new Activer("sonarist"));

        List<Action> legal = ActivationDriver.legalActions(state);
        assertTrue(legal.contains(new Sonar(new Cell(1, 0), 0)), "la piste à récompense est offerte");
        assertTrue(legal.contains(new Sonar(new Cell(1, 1), 0)), "la piste en découverte l'est aussi");
    }

    @Test
    void leSonarPoseUnDisqueEncaisseLaRecompenseEtConsommeLaChaine() {
        GameState state = readyToSonar(List.of(Gain.RESEARCH), 2, List.of(SONAR_SLOT));
        ActivationDriver.apply(state, new Activer("sonarist"));
        int researchBefore = state.player(0).research();

        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));

        assertEquals(0, state.player(0).transitDiscs(), "le disque d'activation puis celui du Sonar sont dépensés");
        assertEquals(1, state.oceanBoard().sonarDiscCount(new Cell(1, 0), 0), "un disque posé sur la piste");
        assertEquals(researchBefore + 1, state.player(0).research(), "la récompense (recherche) est encaissée");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state),
                "chaîne d'un seul emplacement épuisée");
    }

    @Test
    void sansDisqueDeTransitAucunSonarNEstPropose() {
        GameState state = readyToSonar(List.of(Gain.RESEARCH), 1, List.of(SONAR_SLOT));
        ActivationDriver.apply(state, new Activer("sonarist")); // consomme l'unique disque de transit

        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void leSonarSurCaseRecompensePeutDeclencherLaCascadeDePoseDImpact() {
        GameState state = readyToSonar(List.of(Gain.IMPACT), 2, List.of(SONAR_SLOT));
        ActivationDriver.apply(state, new Activer("sonarist"));

        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));

        // impact gagné : le tour est suspendu sur la pose
        assertEquals(List.of(new PoserImpact(0, 0)), ActivationDriver.legalActions(state));
        ActivationDriver.apply(state, new PoserImpact(0, 0));
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void apresLaCaseRecompenseLaCaseDecouverteDeLaMemePisteResteOfferte() {
        GameState state = readyToSonar(List.of(Gain.RESEARCH), 3, List.of(SONAR_SLOT, SONAR_SLOT));
        ActivationDriver.apply(state, new Activer("sonarist"));
        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0)); // remplit la case récompense

        // la case libre suivante de la piste est une découverte : la piste reste offerte
        assertTrue(ActivationDriver.legalActions(state).contains(new Sonar(new Cell(1, 0), 0)));
    }

    @Test
    void leSonarAvantActivationEstRefuse() {
        GameState state = readyToSonar(List.of(Gain.RESEARCH), 2, List.of(SONAR_SLOT));
        assertThrows(IllegalStateException.class,
                () -> ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0)));
    }

    // --- Sonar : découverte -----------------------------------------------

    private static OceanTile discovered(String id, int depth, List<Gain> discoverBonus) {
        return new OceanTile(id, id, depth, false, discoverBonus, List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    /**
     * Joueur 0 en activation, un submersible en (1,0) dont la piste commence par une
     * découverte aux niveaux donnés. La pioche contient {@code pileTiles} (non posées).
     * Plateau de 3 colonnes, pour laisser des cases où poser la tuile découverte.
     */
    private static GameState readyToDiscover(List<Integer> discoverLevels, List<OceanTile> pileTiles) {
        OceanBoard ocean = new OceanBoard(3);
        ocean.placeTile(new Cell(1, 0), "disco-start");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        List<OceanTile> tiles = new ArrayList<>();
        tiles.add(new OceanTile("disco-start", "Disco Start", 1, false, List.of(), List.of(), List.of(),
                List.of(new SonarTrack(List.of(new SonarSpot.Discover(discoverLevels)))), List.of(), List.of()));
        tiles.addAll(pileTiles);
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, new OceanTileCatalog(tiles), Fixtures.diveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(sonarSpecialist(List.of(SONAR_SLOT))));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        return state;
    }

    @Test
    void leSonarSurDecouverteTireDeuxTuilesEtSuspendSurLeChoix() {
        GameState state = readyToDiscover(List.of(1), List.of(
                discovered("found-a", 1, List.of(Gain.RESEARCH)),
                discovered("found-b", 1, List.of(Gain.RESEARCH))));
        ActivationDriver.apply(state, new Activer("sonarist"));

        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));

        assertEquals(0, state.player(0).transitDiscs(), "disque d'activation + disque de Sonar dépensés");
        assertEquals(1, state.oceanBoard().sonarDiscCount(new Cell(1, 0), 0), "disque posé sur la piste de découverte");
        List<Action> legal = ActivationDriver.legalActions(state);
        assertEquals(2, legal.size(), "deux tuiles à départager");
        assertTrue(legal.contains(new GarderTuile("found-a")));
        assertTrue(legal.contains(new GarderTuile("found-b")));
    }

    @Test
    void garderPuisPoserPlaceLaTuileEncaisseSonBonusEtRendLautreALaPioche() {
        GameState state = readyToDiscover(List.of(1), List.of(
                discovered("found-a", 1, List.of(Gain.RESEARCH)),
                discovered("found-b", 1, List.of(Gain.RESEARCH))));
        ActivationDriver.apply(state, new Activer("sonarist"));
        int pileBefore = state.discoveryPile().size();
        int researchBefore = state.player(0).research();
        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));

        ActivationDriver.apply(state, new GarderTuile("found-a"));
        List<Action> placements = ActivationDriver.legalActions(state);
        assertTrue(placements.contains(new PoserTuile(new Cell(1, 1))), "cases libres de profondeur 1 proposées");
        assertTrue(placements.contains(new PoserTuile(new Cell(1, 2))));

        ActivationDriver.apply(state, new PoserTuile(new Cell(1, 1)));

        assertEquals("found-a", state.oceanBoard().tileAt(new Cell(1, 1)).orElseThrow(), "tuile gardée posée");
        assertEquals(researchBefore + 1, state.player(0).research(), "bonus de découverte encaissé");
        assertEquals(pileBefore - 1, state.discoveryPile().size(), "gardée retirée, non gardée rendue à la pioche");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state),
                "pas de Sonar en réserve, chaîne épuisée");
    }

    @Test
    void uneTuileProfondeSePoseSeulementSousUneZoneExistante() {
        GameState state = readyToDiscover(List.of(2), List.of(discovered("deep-a", 2, List.of(Gain.RESEARCH))));
        ActivationDriver.apply(state, new Activer("sonarist"));
        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));
        ActivationDriver.apply(state, new GarderTuile("deep-a"));

        // seule (2,0) est valide : (1,0) est occupée au-dessus ; (2,1)/(2,2) n'ont pas de zone au-dessus
        assertEquals(List.of(new PoserTuile(new Cell(2, 0))), ActivationDriver.legalActions(state));
    }

    @Test
    void leBonusDeDecouvertePeutDeclencherLaCascadeDePoseDImpact() {
        GameState state = readyToDiscover(List.of(1), List.of(discovered("found-impact", 1, List.of(Gain.IMPACT))));
        ActivationDriver.apply(state, new Activer("sonarist"));
        ActivationDriver.apply(state, new Sonar(new Cell(1, 0), 0));
        ActivationDriver.apply(state, new GarderTuile("found-impact"));
        ActivationDriver.apply(state, new PoserTuile(new Cell(1, 1)));

        // le bonus de découverte donne un impact : le tour est suspendu sur la pose
        assertEquals(List.of(new PoserImpact(0, 0)), ActivationDriver.legalActions(state));
        ActivationDriver.apply(state, new PoserImpact(0, 0));
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    // --- Plongée ------------------------------------------------------------

    private static final ActionSlot DIVE_SLOT = new ActionSlot(List.of(ActionType.DIVE));

    /** Un spécialiste dont la chaîne offre {@code slots} emplacements Plongée. */
    private static Specialist diveSpecialist(List<ActionSlot> chain) {
        SpecialistSide junior = new SpecialistSide("Diver", List.of(), chain, Optional.empty(), Optional.empty());
        SpecialistSide senior = new SpecialistSide("Diver S", List.of(), List.of(),
                Optional.empty(), Optional.empty());
        return new Specialist("diver", OptionalInt.of(1), false, junior, senior);
    }

    /**
     * Deux types de jeton : {@code research} (gains purs), {@code sonar-token} (3
     * recherche OU un Voyage accordé). Le Voyage sert de terrain neutre pour éprouver
     * une action déclenchée par jeton (pas de coût, pas de piste à choisir).
     */
    private static DiveTokenCatalog testDiveTokenCatalog() {
        return new DiveTokenCatalog(List.of(
                new DiveToken("research", 6,
                        List.of(new DiveOption.Gains(List.of(Gain.RESEARCH, Gain.RESEARCH), List.of()))),
                new DiveToken("sonar-token", 2, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH), List.of()),
                        new DiveOption.TriggersAction(ActionType.TRAVEL, OptionalInt.empty())))));
    }

    /**
     * Joueur 0 en activation avec un spécialiste Plongée, un submersible en (1,0) dont
     * le site {@code d1} porte exactement {@code stackedTokens} (empilés à la main pour
     * un ordre déterministe — le sommet est le premier de la liste).
     */
    private static GameState readyToDiveChain(List<String> stackedTokens, List<ActionSlot> chain,
                                              DiveTokenCatalog diveTokens) {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "dive-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        ocean.stackDiveTokens(new Cell(1, 0), "d1", stackedTokens);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("dive-tile", "Dive Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", Math.max(1, stackedTokens.size()))), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, diveTokens);
        state.player(0).recruit(HeldSpecialist.recruited(diveSpecialist(chain)));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        return state;
    }

    private static GameState readyToDive(List<String> stackedTokens, DiveTokenCatalog diveTokens) {
        return readyToDiveChain(stackedTokens, List.of(DIVE_SLOT), diveTokens);
    }

    @Test
    void unSpecialisteDePlongeeProposeLaPlongeeSiLeSiteAUnJeton() {
        GameState state = readyToDive(List.of("research"), testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));

        assertTrue(ActivationDriver.legalActions(state).contains(new Dive(new Cell(1, 0), "d1")));
    }

    @Test
    void sansJetonAucunePlongeeNEstProposee() {
        GameState state = readyToDive(List.of(), testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));

        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void laPlongeeNeCouteRienEnDisque() {
        GameState state = readyToDive(List.of("research"), testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));
        int transitBefore = state.player(0).transitDiscs();

        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        assertEquals(transitBefore, state.player(0).transitDiscs(), "aucun disque dépensé par la Plongée");
    }

    @Test
    void laPlongeePrendLeJetonDuSommetEtConsommeLaChaine() {
        GameState state = readyToDive(List.of("research", "sonar-token"), testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));

        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        assertEquals(List.of("research"), state.player(0).heldDiveTokens(), "le jeton du sommet rejoint la main");
        assertEquals(1, state.oceanBoard().diveTokenCount(new Cell(1, 0), "d1"), "un jeton restant sur le site");
        assertEquals(List.of(new DepenserJeton(0, 0), new TerminerTour(), new Passer()),
                ActivationDriver.legalActions(state), "chaîne épuisée, mais le jeton pris reste à dépenser");
    }

    @Test
    void laPlongeeAvantActivationEstRefusee() {
        GameState state = readyToDive(List.of("research"), testDiveTokenCatalog());
        assertThrows(IllegalStateException.class,
                () -> ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1")));
    }

    // --- Dépense d'un jeton de plongée --------------------------------------

    @Test
    void depenserUnJetonDeGainsLeResoutEntierement() {
        GameState state = readyToDive(List.of("research"), testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));
        int researchBefore = state.player(0).research();

        ActivationDriver.apply(state, new DepenserJeton(0, 0));

        assertEquals(researchBefore + 2, state.player(0).research());
        assertTrue(state.player(0).heldDiveTokens().isEmpty(), "le jeton dépensé quitte la main");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void unJetonQuiDeclencheUneActionSuspendLeTourSurCetteSeuleAction() {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "dive-tile");
        ocean.placeTile(new Cell(1, 1), "dest-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        ocean.stackDiveTokens(new Cell(1, 0), "d1", List.of("sonar-token"));
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("dive-tile", "Dive Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", 1)), List.of()),
                new OceanTile("dest-tile", "Dest Tile", 1, false, List.of(), List.of(Gain.RESEARCH), List.of(),
                        List.of(), List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, testDiveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(diveSpecialist(List.of(DIVE_SLOT))));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        ActivationDriver.apply(state, new DepenserJeton(0, 1)); // option 1 = Voyage accordé

        assertEquals(List.of(new Voyager(new Cell(1, 0), new Cell(1, 1))), ActivationDriver.legalActions(state),
                "le tour se suspend sur le seul Voyage accordé");

        int researchBefore = state.player(0).research();
        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        assertEquals(researchBefore + 1, state.player(0).research(), "bonus d'arrivée encaissé normalement");
        assertTrue(state.player(0).heldDiveTokens().isEmpty());
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state),
                "la chaîne du spécialiste (déjà épuisée par la Plongée) n'a pas avancé pour autant");
    }

    @Test
    void unJetonEnMainPeutRemplacerEntierementLActivation() {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "dive-tile");
        ocean.placeTile(new Cell(1, 1), "dest-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("dive-tile", "Dive Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of()),
                new OceanTile("dest-tile", "Dest Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, testDiveTokenCatalog());
        state.player(0).receiveDiveToken("sonar-token"); // déjà en main, conservé d'un tour précédent
        ActivationDriver.begin(state);

        assertTrue(ActivationDriver.legalActions(state).contains(new DepenserJeton(0, 1)),
                "la dépense est offerte sans avoir activé de spécialiste");

        ActivationDriver.apply(state, new DepenserJeton(0, 1));
        assertEquals(List.of(new Voyager(new Cell(1, 0), new Cell(1, 1))), ActivationDriver.legalActions(state));

        ActivationDriver.apply(state, new Voyager(new Cell(1, 0), new Cell(1, 1)));

        assertEquals(1, state.oceanBoard().vesselCount(new Cell(1, 1), 0), "le Voyage a eu lieu sans activation");
        assertEquals(List.of(new Passer()), ActivationDriver.legalActions(state),
                "aucune activation : pas de TerminerTour, et aucun disque de transit pour en activer une autre");
    }

    @Test
    void depenserUnAutreJetonPendantQuUneActionResteAJouerEstRefuse() {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "dive-tile");
        ocean.placeTile(new Cell(1, 1), "dest-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        ocean.stackDiveTokens(new Cell(1, 0), "d1", List.of("sonar-token"));
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("dive-tile", "Dive Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", 1)), List.of()),
                new OceanTile("dest-tile", "Dest Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of())));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, testDiveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(diveSpecialist(List.of(DIVE_SLOT))));
        state.player(0).moveReserveToTransit(2);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));
        ActivationDriver.apply(state, new DepenserJeton(0, 1)); // suspend le tour sur un Voyage

        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new DepenserJeton(0, 0)));
    }

    @Test
    void auPlusUnJetonPeutEtreConserveEnFinDeTour() {
        GameState state = readyToDiveChain(List.of("research", "research"), List.of(DIVE_SLOT, DIVE_SLOT),
                testDiveTokenCatalog());
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        assertEquals(2, state.player(0).heldDiveTokens().size());
        List<Action> legal = ActivationDriver.legalActions(state);
        assertFalse(legal.contains(new TerminerTour()), "2 jetons en main : la fin de tour est bloquée");
        assertFalse(legal.contains(new Passer()));
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new TerminerTour()));
        assertThrows(IllegalStateException.class, () -> ActivationDriver.apply(state, new Passer()));

        ActivationDriver.apply(state, new DepenserJeton(0, 0)); // dépense l'un des deux

        assertTrue(ActivationDriver.legalActions(state).contains(new TerminerTour()),
                "un seul jeton restant : la fin de tour redevient possible");
    }

    @Test
    void desOptionsNonSupporteesNeSontJamaisProposees() {
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(
                new DiveToken("mixed", 4, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()),
                        new DiveOption.Gains(List.of(Gain.ANY_ATTRIBUTE), List.of()),
                        new DiveOption.TriggersAction(ActionType.CONSERVE, OptionalInt.empty())))));
        GameState state = readyToDive(List.of("mixed"), catalog);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        assertEquals(List.of(new DepenserJeton(0, 0), new TerminerTour(), new Passer()),
                ActivationDriver.legalActions(state),
                "seule l'option de recherche pure est jouable pour l'instant");
    }

    @Test
    void depenserUnJetonPeutPayerUnCoutEnDisque() {
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(
                new DiveToken("cancel-disc", 2, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH), List.of()),
                        new DiveOption.Gains(
                                List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH, Gain.RESEARCH),
                                List.of(Gain.DISC))))));
        GameState state = readyToDive(List.of("cancel-disc"), catalog);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));
        int reserveBefore = state.player(0).reserveDiscs();
        int researchBefore = state.player(0).research();

        ActivationDriver.apply(state, new DepenserJeton(0, 1));

        assertEquals(reserveBefore - 1, state.player(0).reserveDiscs(), "un disque de réserve payé");
        assertEquals(researchBefore + 5, state.player(0).research());
    }

    @Test
    void uneOptionAvecCoutNEstPasOfferteSiOnNePeutPasPayer() {
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(
                new DiveToken("cancel-disc", 2, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()),
                        new DiveOption.Gains(List.of(Gain.RESEARCH, Gain.RESEARCH), List.of(Gain.DISC))))));
        GameState state = readyToDive(List.of("cancel-disc"), catalog);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));
        while (state.player(0).reserveDiscs() > 0) {
            state.player(0).spendReserveDisc();
        }

        assertFalse(ActivationDriver.legalActions(state).contains(new DepenserJeton(0, 1)),
                "coût impayable : l'option n'est pas offerte");
    }

    @Test
    void unJetonPeutDeclencherUneAutrePlongee() {
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(
                new DiveToken("dive-token", 2, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()),
                        new DiveOption.TriggersAction(ActionType.DIVE, OptionalInt.empty())))));
        GameState state = readyToDive(List.of("dive-token", "research"), catalog);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1")); // prend dive-token ; research reste sur le site

        ActivationDriver.apply(state, new DepenserJeton(0, 1)); // déclenche une nouvelle Plongée

        assertEquals(List.of(new Dive(new Cell(1, 0), "d1")), ActivationDriver.legalActions(state));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        assertEquals(List.of("research"), state.player(0).heldDiveTokens(), "le second jeton pris rejoint la main");
    }

    // --- Conservation ---------------------------------------------------------

    private static final ActionSlot CONSERVE_SLOT = new ActionSlot(List.of(ActionType.CONSERVE));

    /** Un spécialiste dont la chaîne offre {@code slots} emplacements Conservation. */
    private static Specialist conservationSpecialist(List<ActionSlot> chain) {
        SpecialistSide junior = new SpecialistSide("Curator", List.of(), chain, Optional.empty(), Optional.empty());
        SpecialistSide senior = new SpecialistSide("Curator S", List.of(), List.of(),
                Optional.empty(), Optional.empty());
        return new Specialist("curator", OptionalInt.of(1), false, junior, senior);
    }

    /**
     * Joueur 0 en activation avec un spécialiste Conservation, un submersible en
     * (1,0) dont la tuile porte le site {@code c1} (coût {@code cost}, gains
     * {@code gains}), {@code research} points de recherche déjà acquis, et
     * {@code transitDiscs} disques en transit.
     */
    private static GameState readyToConserve(int cost, List<Gain> gains, int research, int transitDiscs,
                                             List<ActionSlot> chain) {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "conserve-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("conserve-tile", "Conserve Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(new ConservationSite("c1", cost, gains)))));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, Fixtures.diveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(conservationSpecialist(chain)));
        state.player(0).moveReserveToTransit(transitDiscs);
        state.player(0).gainResearch(research);
        ActivationDriver.begin(state);
        return state;
    }

    @Test
    void unSpecialisteDeConservationProposeLeSiteLibreEtPayable() {
        GameState state = readyToConserve(2, List.of(Gain.REPUTATION), 3, 2, List.of(CONSERVE_SLOT));
        ActivationDriver.apply(state, new Activer("curator"));

        assertTrue(ActivationDriver.legalActions(state).contains(new Conserver(new Cell(1, 0), "c1")));
    }

    @Test
    void sansDisqueDeTransitAucuneConservationNEstProposee() {
        GameState state = readyToConserve(0, List.of(Gain.REPUTATION), 0, 1, List.of(CONSERVE_SLOT));
        ActivationDriver.apply(state, new Activer("curator")); // consomme l'unique disque de transit

        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void uneRechercheInsuffisanteNOffrePasLaConservation() {
        GameState state = readyToConserve(3, List.of(Gain.REPUTATION), 2, 2, List.of(CONSERVE_SLOT));
        ActivationDriver.apply(state, new Activer("curator"));

        assertFalse(ActivationDriver.legalActions(state).contains(new Conserver(new Cell(1, 0), "c1")));
    }

    @Test
    void laConservationPaieLeCoutPoseUnDisqueEtEncaisseLesGains() {
        GameState state = readyToConserve(2, List.of(Gain.REPUTATION), 3, 2, List.of(CONSERVE_SLOT));
        ActivationDriver.apply(state, new Activer("curator"));
        int reputationBefore = state.player(0).attributes().step(Attribute.REPUTATION);

        ActivationDriver.apply(state, new Conserver(new Cell(1, 0), "c1"));

        assertEquals(1, state.player(0).research(), "coût de 2 payé sur 3");
        assertEquals(0, state.player(0).transitDiscs(),
                "le disque d'activation puis celui de la Conservation sont dépensés");
        assertTrue(state.oceanBoard().conservationSiteOccupied(new Cell(1, 0), "c1"), "le site porte un disque");
        assertEquals(reputationBefore + 1, state.player(0).attributes().step(Attribute.REPUTATION),
                "la récompense (réputation) est encaissée");
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state),
                "chaîne d'un seul emplacement épuisée");
    }

    @Test
    void unSiteDejaOccupeNEstPlusOffertMaisLesAutresRestent() {
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "conserve-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        OceanTileCatalog catalog = new OceanTileCatalog(List.of(
                new OceanTile("conserve-tile", "Conserve Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of(
                                new ConservationSite("c1", 0, List.of()),
                                new ConservationSite("c2", 0, List.of())))));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, catalog, Fixtures.diveTokenCatalog());
        state.player(0).recruit(HeldSpecialist.recruited(
                conservationSpecialist(List.of(CONSERVE_SLOT, CONSERVE_SLOT))));
        state.player(0).moveReserveToTransit(3);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("curator"));

        ActivationDriver.apply(state, new Conserver(new Cell(1, 0), "c1"));

        List<Action> legal = ActivationDriver.legalActions(state);
        assertFalse(legal.contains(new Conserver(new Cell(1, 0), "c1")), "site déjà occupé");
        assertTrue(legal.contains(new Conserver(new Cell(1, 0), "c2")), "l'autre site reste libre");
    }

    @Test
    void laConservationPeutDeclencherLaCascadeDePoseDImpact() {
        GameState state = readyToConserve(0, List.of(Gain.IMPACT), 0, 2, List.of(CONSERVE_SLOT));
        ActivationDriver.apply(state, new Activer("curator"));

        ActivationDriver.apply(state, new Conserver(new Cell(1, 0), "c1"));

        // impact gagné : le tour est suspendu sur la pose
        assertEquals(List.of(new PoserImpact(0, 0)), ActivationDriver.legalActions(state));
        ActivationDriver.apply(state, new PoserImpact(0, 0));
        assertEquals(List.of(new TerminerTour(), new Passer()), ActivationDriver.legalActions(state));
    }

    @Test
    void laConservationAvantActivationEstRefusee() {
        GameState state = readyToConserve(0, List.of(Gain.REPUTATION), 0, 2, List.of(CONSERVE_SLOT));
        assertThrows(IllegalStateException.class,
                () -> ActivationDriver.apply(state, new Conserver(new Cell(1, 0), "c1")));
    }

    @Test
    void unJetonPeutDeclencherUneConservation() {
        DiveTokenCatalog catalog = new DiveTokenCatalog(List.of(
                new DiveToken("conserve-token", 2, List.of(
                        new DiveOption.Gains(List.of(Gain.RESEARCH), List.of()),
                        new DiveOption.TriggersAction(ActionType.CONSERVE, OptionalInt.empty())))));
        OceanBoard ocean = new OceanBoard(2);
        ocean.placeTile(new Cell(1, 0), "dive-tile");
        ocean.addVessels(new Cell(1, 0), 0, 1);
        ocean.stackDiveTokens(new Cell(1, 0), "d1", List.of("conserve-token"));
        OceanTileCatalog oceanCatalog = new OceanTileCatalog(List.of(
                new OceanTile("dive-tile", "Dive Tile", 1, false, List.of(), List.of(), List.of(), List.of(),
                        List.of(new DiveSite("d1", 1)),
                        List.of(new ConservationSite("c1", 1, List.of(Gain.REPUTATION))))));
        GameState state = GameState.newGame(1, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), ocean, oceanCatalog, catalog);
        state.player(0).recruit(HeldSpecialist.recruited(diveSpecialist(List.of(DIVE_SLOT))));
        state.player(0).moveReserveToTransit(2);
        state.player(0).gainResearch(1);
        ActivationDriver.begin(state);
        ActivationDriver.apply(state, new Activer("diver"));
        ActivationDriver.apply(state, new Dive(new Cell(1, 0), "d1"));

        ActivationDriver.apply(state, new DepenserJeton(0, 1)); // déclenche une Conservation

        assertEquals(List.of(new Conserver(new Cell(1, 0), "c1")), ActivationDriver.legalActions(state),
                "le tour se suspend sur la seule Conservation accordée");
        ActivationDriver.apply(state, new Conserver(new Cell(1, 0), "c1"));

        assertTrue(state.oceanBoard().conservationSiteOccupied(new Cell(1, 0), "c1"));
        assertTrue(state.player(0).heldDiveTokens().isEmpty());
    }
}
