package io.github.yoyodes1000.endeavor.engine.specialist;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Petits assembleurs de tuiles valides, pour ne pas répéter la plomberie dans chaque test. */
final class SpecialistFixtures {

    private SpecialistFixtures() {
    }

    static SpecialistSide plainSide(String name) {
        return new SpecialistSide(
                name,
                List.of(Gain.INSPIRATION),
                List.of(new ActionSlot(List.of(ActionType.TRAVEL))),
                Optional.empty(),
                Optional.empty());
    }

    static SpecialistSide seniorWithEndGame(String name) {
        return new SpecialistSide(
                name,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(new EndGameScoring("décompte", 1, 1, EndGameCount.UNCONDITIONAL)));
    }

    static Specialist teamLeader(String id) {
        return new Specialist(id, OptionalInt.empty(), true, plainSide("Team Leader"), plainSide("Team Leader"));
    }

    static Specialist teamLeader() {
        return teamLeader("team-leader");
    }

    static Specialist ranked(String id, int rank) {
        return new Specialist(id, OptionalInt.of(rank), false, plainSide(id + "-j"), plainSide(id + "-s"));
    }

    static Specialist rankFive(String id) {
        return new Specialist(id, OptionalInt.of(5), false, plainSide(id + "-j"), seniorWithEndGame(id + "-s"));
    }
}
