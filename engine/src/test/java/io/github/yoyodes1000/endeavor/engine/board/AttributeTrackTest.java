package io.github.yoyodes1000.endeavor.engine.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttributeTrackTest {

    @Test
    void leNiveauSeDeduitDeLaCaseSelonLesPaliers() {
        int[] attendu = {1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5};
        for (int step = 0; step <= 12; step++) {
            assertEquals(attendu[step], new AttributeTrack(step).level(), "case " + step);
        }
    }

    @Test
    void lesPointsSuiventLePalier() {
        assertEquals(0, new AttributeTrack(0).points());   // niveau 1
        assertEquals(1, new AttributeTrack(2).points());   // niveau 2
        assertEquals(4, new AttributeTrack(4).points());   // niveau 3
        assertEquals(7, new AttributeTrack(7).points());   // niveau 4
        assertEquals(10, new AttributeTrack(10).points()); // niveau 5
    }

    @Test
    void auDepartCaseZeroNiveauUn() {
        AttributeTrack depart = AttributeTrack.start();
        assertEquals(0, depart.step());
        assertEquals(1, depart.level());
    }

    @Test
    void avanceDansLesBornes() {
        assertEquals(new AttributeTrack(3), new AttributeTrack(0).advancedBy(3));
        assertEquals(12, new AttributeTrack(9).advancedBy(3).step());
    }

    @Test
    void avancerDeZeroNeChangeRien() {
        assertEquals(new AttributeTrack(5), new AttributeTrack(5).advancedBy(0));
    }

    @Test
    void leDepassementRameneALaCaseDix() {
        assertEquals(10, new AttributeTrack(12).advancedBy(1).step());
        assertEquals(10, new AttributeTrack(8).advancedBy(6).step());  // 14 -> 10
        assertEquals(10, new AttributeTrack(10).advancedBy(5).step()); // 15 -> 10
    }

    @Test
    void refuseUneCaseHorsBornes() {
        assertThrows(IllegalArgumentException.class, () -> new AttributeTrack(-1));
        assertThrows(IllegalArgumentException.class, () -> new AttributeTrack(13));
    }

    @Test
    void refuseUneAvanceNegative() {
        assertThrows(IllegalArgumentException.class, () -> new AttributeTrack(5).advancedBy(-1));
    }
}
