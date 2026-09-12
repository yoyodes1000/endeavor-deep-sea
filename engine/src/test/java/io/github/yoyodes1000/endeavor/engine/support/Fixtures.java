package io.github.yoyodes1000.endeavor.engine.support;

import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Fixtures partagées entre les tests des différents packages : tuiles minimales
 * et petit casier valide, pour tester l'état et les étapes sans dépendre du
 * chargeur.
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static SpecialistSide side(String name) {
        return new SpecialistSide(name, List.of(), List.of(), Optional.empty(), Optional.empty());
    }

    public static Specialist teamLeader() {
        return new Specialist("team-leader", OptionalInt.empty(), true, side("Team Leader"), side("Team Leader"));
    }

    public static Specialist ranked(String id, int rank) {
        return new Specialist(id, OptionalInt.of(rank), false, side(id + "-j"), side(id + "-s"));
    }

    /** Chef d'équipe + trois tuiles recrutables. */
    public static SpecialistRoster roster() {
        return new SpecialistRoster(List.of(
                teamLeader(),
                ranked("pilot", 1),
                ranked("ecologist", 2),
                ranked("navigator", 2)));
    }
}
