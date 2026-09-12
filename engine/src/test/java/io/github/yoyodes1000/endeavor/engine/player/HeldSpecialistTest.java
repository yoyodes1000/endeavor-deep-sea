package io.github.yoyodes1000.endeavor.engine.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import org.junit.jupiter.api.Test;

class HeldSpecialistTest {

    @Test
    void unRecrutementEstJuniorSansDisque() {
        Specialist pilote = PlayerFixtures.ranked("pilot");
        HeldSpecialist tuile = HeldSpecialist.recruited(pilote);

        assertEquals(SpecialistFace.JUNIOR, tuile.face());
        assertEquals(0, tuile.placedDiscs());
        assertSame(pilote.junior(), tuile.activeSide());
    }

    @Test
    void laFaceSeniorExposeLeCoteSenior() {
        Specialist pilote = PlayerFixtures.ranked("pilot");
        HeldSpecialist tuile = new HeldSpecialist(pilote, SpecialistFace.SENIOR, 2);

        assertSame(pilote.senior(), tuile.activeSide());
        assertEquals(2, tuile.placedDiscs());
    }

    @Test
    void refuseUnNombreDeDisquesNegatif() {
        assertThrows(IllegalArgumentException.class,
                () -> new HeldSpecialist(PlayerFixtures.ranked("x"), SpecialistFace.JUNIOR, -1));
    }
}
