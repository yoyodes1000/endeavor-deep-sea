package io.github.yoyodes1000.endeavor.engine.player;

import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Tuiles minimales pour tester le plateau joueur sans dépendre du chargeur. */
final class PlayerFixtures {

    private PlayerFixtures() {
    }

    static SpecialistSide side(String name) {
        return new SpecialistSide(name, List.of(), List.of(), Optional.empty(), Optional.empty());
    }

    static Specialist teamLeader() {
        return new Specialist("team-leader", OptionalInt.empty(), true, side("Team Leader"), side("Team Leader"));
    }

    static Specialist ranked(String id) {
        return new Specialist(id, OptionalInt.of(1), false, side(id + "-j"), side(id + "-s"));
    }
}
