package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Un petit casier valide pour tester l'état sans dépendre du chargeur. */
final class GameFixtures {

    private GameFixtures() {
    }

    /** Un plateau de mission minimal : une seule case de départ, sans gain. */
    static MissionBoard missionBoard() {
        ImpactHex start = new ImpactHex(0, 0, 0, List.of(), true, false, false);
        return new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, List.of(start)));
    }

    /** Un océan de départ minimal : quelques zones adjacentes (surface + une descente). */
    static OceanBoard oceanBoard() {
        OceanBoard board = new OceanBoard(3);
        board.placeTile(new Cell(1, 0), "atoll");
        board.placeTile(new Cell(1, 1), "reef");
        board.placeTile(new Cell(2, 1), "trench");
        return board;
    }

    /** Le catalogue des tuiles de l'océan minimal, avec des bonus d'arrivée simples. */
    static OceanTileCatalog oceanCatalog() {
        return new OceanTileCatalog(List.of(
                new OceanTile("atoll", "Atoll", 1, false, List.of(), List.of(Gain.INSPIRATION), List.of(), List.of()),
                new OceanTile("reef", "Reef", 1, false, List.of(), List.of(Gain.RESEARCH), List.of(), List.of()),
                new OceanTile("trench", "Trench", 2, false, List.of(), List.of(Gain.DISC), List.of(), List.of())));
    }

    /** Une partie neuve sur le plateau de mission minimal, pour alléger les tests. */
    static GameState newGame(int players, long seed) {
        return GameState.newGame(players, roster(), RandomSource.fromSeed(seed), 10,
                missionBoard(), oceanBoard(), oceanCatalog());
    }

    static SpecialistSide side(String name) {
        return new SpecialistSide(name, List.of(), List.of(), Optional.empty(), Optional.empty());
    }

    static Specialist teamLeader() {
        return new Specialist("team-leader", OptionalInt.empty(), true, side("Team Leader"), side("Team Leader"));
    }

    static Specialist ranked(String id, int rank) {
        return new Specialist(id, OptionalInt.of(rank), false, side(id + "-j"), side(id + "-s"));
    }

    /** Chef d'équipe + trois tuiles recrutables. */
    static SpecialistRoster roster() {
        return new SpecialistRoster(List.of(
                teamLeader(),
                ranked("pilot", 1),
                ranked("ecologist", 2),
                ranked("navigator", 2)));
    }
}
