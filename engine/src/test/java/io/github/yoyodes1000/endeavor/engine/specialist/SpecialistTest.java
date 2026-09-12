package io.github.yoyodes1000.endeavor.engine.specialist;

import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.plainSide;
import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.ranked;
import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.rankFive;
import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.seniorWithEndGame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SpecialistTest {

    @Test
    void accepteUnSpecialisteAvecUnRangValide() {
        assertDoesNotThrow(() -> ranked("pilot", 1));
    }

    @Test
    void accepteLeChefDEquipeSansRang() {
        assertDoesNotThrow(() -> SpecialistFixtures.teamLeader());
    }

    @Test
    void accepteUnRangCinqAvecDecompteCoteSenior() {
        assertDoesNotThrow(() -> rankFive("mentor"));
    }

    @Test
    void refuseUnChefDEquipeAvecUnRang() {
        assertThrows(IllegalArgumentException.class, () ->
                new Specialist("team-leader", OptionalInt.of(1), true, plainSide("a"), plainSide("b")));
    }

    @Test
    void refuseUnNonChefSansRang() {
        assertThrows(IllegalArgumentException.class, () ->
                new Specialist("x", OptionalInt.empty(), false, plainSide("a"), plainSide("b")));
    }

    @Test
    void refuseUnRangInferieurAUn() {
        assertThrows(IllegalArgumentException.class, () -> ranked("x", 0));
    }

    @Test
    void refuseUnRangSuperieurACinq() {
        assertThrows(IllegalArgumentException.class, () -> ranked("x", 6));
    }

    @Test
    void refuseUnRangCinqSansDecompte() {
        assertThrows(IllegalArgumentException.class, () ->
                new Specialist("x", OptionalInt.of(5), false, plainSide("j"), plainSide("s")));
    }

    @Test
    void refuseUnDecompteSurUnRangInferieur() {
        assertThrows(IllegalArgumentException.class, () ->
                new Specialist("x", OptionalInt.of(4), false, plainSide("j"), seniorWithEndGame("s")));
    }

    @Test
    void refuseUnDecompteSurLaFaceJunior() {
        SpecialistSide juniorAvecDecompte = seniorWithEndGame("j");
        assertThrows(IllegalArgumentException.class, () ->
                new Specialist("x", OptionalInt.of(5), false, juniorAvecDecompte, seniorWithEndGame("s")));
    }
}
