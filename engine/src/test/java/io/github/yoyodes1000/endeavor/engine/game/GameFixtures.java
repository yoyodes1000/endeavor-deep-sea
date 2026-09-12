package io.github.yoyodes1000.endeavor.engine.game;

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
