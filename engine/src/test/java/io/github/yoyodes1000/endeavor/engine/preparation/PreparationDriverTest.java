package io.github.yoyodes1000.endeavor.engine.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.Recuperer;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.game.PreparationCursor.Step;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class PreparationDriverTest {

    // --- Fabriques de matériel minimal ------------------------------------

    private static SpecialistSide side(String name, Gain... gains) {
        return new SpecialistSide(name, List.of(gains), List.of(), Optional.empty(), Optional.empty());
    }

    /** Une tuile recrutable d'un rang donné, avec ses gains immédiats côté Junior. */
    private static Specialist tile(String id, int rank, Gain... juniorGains) {
        return new Specialist(id, OptionalInt.of(rank), false, side(id + "-j", juniorGains), side(id + "-s"));
    }

    private static SpecialistRoster roster(Specialist... recruitables) {
        List<Specialist> all = new ArrayList<>();
        all.add(Fixtures.teamLeader());
        all.addAll(List.of(recruitables));
        return new SpecialistRoster(all);
    }

    /** Un plateau de {@code count} cases de départ alignées, toutes sans gain. */
    private static MissionBoard boardWithStarts(int count) {
        List<ImpactHex> hexes = new ArrayList<>();
        for (int col = 0; col < count; col++) {
            hexes.add(new ImpactHex(0, col, 0, List.of(), true, false, false));
        }
        return new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, hexes));
    }

    private static GameState game(SpecialistRoster roster, MissionBoard board, int players) {
        return GameState.newGame(players, roster, RandomSource.fromSeed(1), 10, board, Fixtures.oceanBoard());
    }

    // --- Tests ------------------------------------------------------------

    @Test
    void beginFixeLePremierJoueurEtSArreteSurLeRecrutement() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);

        PreparationDriver.begin(state);

        assertEquals(Step.RECRUIT, state.cursor().step());
        assertEquals(List.of(new Recruter("pilot")), PreparationDriver.legalActions(state));
    }

    @Test
    void beginRefuseUneSecondeEntree() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);
        assertThrows(IllegalStateException.class, () -> PreparationDriver.begin(state));
    }

    @Test
    void unTourSansImpactRecruteFaitLEffortEtTermine() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);

        PreparationDriver.apply(state, new Recruter("pilot"));

        assertEquals(Step.DONE, state.cursor().step());
        assertTrue(PreparationDriver.legalActions(state).isEmpty());
        Player joueur = state.player(0);
        assertEquals(2, joueur.specialists().size(), "chef d'équipe + tuile recrutée");
        assertEquals(1, joueur.transitDiscs(), "effort : 1 disque (inspiration niveau 1)");
        assertEquals(9, joueur.reserveDiscs());
    }

    @Test
    void recruterUneTuileAImpactOuvreLaPoseEtOccupeLHexagone() {
        GameState state = game(roster(tile("impactor", 1, Gain.IMPACT)), Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);

        PreparationDriver.apply(state, new Recruter("impactor"));

        assertEquals(Step.PLACE_IMPACT, state.cursor().step());
        assertEquals(List.of(new PoserImpact(0, 0)), PreparationDriver.legalActions(state));

        PreparationDriver.apply(state, new PoserImpact(0, 0));

        ImpactHex depart = state.missionBoard().board().hexAt(0, 0).orElseThrow();
        assertTrue(state.missionBoard().isOccupied(depart));
        assertEquals(0, state.missionBoard().owner(depart).orElseThrow());
        assertEquals(Step.DONE, state.cursor().step());
    }

    @Test
    void uneCascadeDeDeuxImpactsSePoseSurDeuxHexagones() {
        GameState state = game(roster(tile("double", 1, Gain.IMPACT, Gain.IMPACT)), boardWithStarts(2), 1);
        PreparationDriver.begin(state);
        PreparationDriver.apply(state, new Recruter("double"));

        assertEquals(2, state.cursor().pendingImpacts());
        PreparationDriver.apply(state, new PoserImpact(0, 0));
        assertEquals(1, state.cursor().pendingImpacts());
        PreparationDriver.apply(state, new PoserImpact(0, 1));

        assertEquals(Step.DONE, state.cursor().step());
        assertTrue(state.missionBoard().isOccupied(state.missionBoard().board().hexAt(0, 0).orElseThrow()));
        assertTrue(state.missionBoard().isOccupied(state.missionBoard().board().hexAt(0, 1).orElseThrow()));
    }

    @Test
    void chaqueJoueurRecruteASonTourDansLOrdreDuTour() {
        GameState state = game(roster(tile("pilotA", 1), tile("pilotB", 1)), Fixtures.missionBoard(), 2);
        PreparationDriver.begin(state);

        assertEquals(0, state.cursor().turnPosition());
        assertEquals(2, PreparationDriver.legalActions(state).size(), "les deux tuiles rang 1 sont offertes");

        Action premier = PreparationDriver.legalActions(state).get(0);
        PreparationDriver.apply(state, premier);

        assertEquals(1, state.cursor().turnPosition(), "au joueur suivant");
        assertEquals(Step.RECRUIT, state.cursor().step());
        assertEquals(1, PreparationDriver.legalActions(state).size(), "le casier s'est vidé d'une tuile");

        PreparationDriver.apply(state, PreparationDriver.legalActions(state).get(0));

        assertEquals(Step.DONE, state.cursor().step());
        assertEquals(2, state.player(0).specialists().size());
        assertEquals(2, state.player(1).specialists().size());
        assertTrue(state.casier().isEmpty());
    }

    @Test
    void laRecuperation1cReprendUnDisqueDansLaLimiteDeLaCoordination() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        // simule un disque déjà posé sur un spécialiste (comme après une Phase 2)
        state.player(0).recruit(new HeldSpecialist(tile("veteran", 1), SpecialistFace.JUNIOR, 2));

        PreparationDriver.begin(state);
        PreparationDriver.apply(state, new Recruter("pilot"));

        assertEquals(Step.RECOVER, state.cursor().step());
        assertEquals(List.of(new Recuperer("veteran")), PreparationDriver.legalActions(state));

        PreparationDriver.apply(state, new Recuperer("veteran"));

        assertEquals(Step.DONE, state.cursor().step());
        HeldSpecialist veteran = state.player(0).specialists().stream()
                .filter(held -> held.specialist().id().equals("veteran")).findFirst().orElseThrow();
        assertEquals(1, veteran.placedDiscs(), "coordination niveau 1 : un seul disque repris");
        assertEquals(2, state.player(0).transitDiscs(), "effort (1) + récupération (1)");
    }

    @Test
    void lesSubmersiblesGagnesSontComptesMaisPasEncoreResolus() {
        GameState state = game(roster(tile("ingenious", 1, Gain.INGENUITY, Gain.INGENUITY)),
                Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);

        PreparationDriver.apply(state, new Recruter("ingenious"));

        assertEquals(Step.DONE, state.cursor().step());
        assertEquals(1, state.cursor().pendingVessels(), "ingéniosité case 2 : un submersible, différé");
    }

    @Test
    void appliquerUnCoupAvantBeginEstRefuse() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        assertThrows(IllegalStateException.class, () -> PreparationDriver.apply(state, new Recruter("pilot")));
    }

    @Test
    void appliquerUnCoupApresLaFinDePhaseEstRefuse() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);
        PreparationDriver.apply(state, new Recruter("pilot"));

        assertEquals(Step.DONE, state.cursor().step());
        assertThrows(IllegalStateException.class, () -> PreparationDriver.apply(state, new Recruter("pilot")));
    }

    @Test
    void unCoupDuMauvaisTypePourLEtapeEstRefuse() {
        GameState state = game(roster(tile("pilot", 1)), Fixtures.missionBoard(), 1);
        PreparationDriver.begin(state);

        assertThrows(IllegalStateException.class, () -> PreparationDriver.apply(state, new PoserImpact(0, 0)));
    }

    @Test
    void unJoueurSansRecrutementLegalSauteLEtape1a() {
        // seule une tuile de rang 2, hors de portée d'une réputation de niveau 1
        GameState state = game(roster(tile("expert", 2)), Fixtures.missionBoard(), 1);

        PreparationDriver.begin(state);

        assertEquals(Step.DONE, state.cursor().step());
        assertEquals(1, state.player(0).specialists().size(), "personne recruté");
        assertEquals(1, state.player(0).transitDiscs(), "l'effort a tout de même eu lieu");
    }
}
