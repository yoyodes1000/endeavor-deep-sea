package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GoalUnitTest {

    @Test
    void traduitChaqueLibelleDesDonnees() {
        assertEquals(GoalUnit.SONAR, GoalUnit.fromCode("sonar"));
        assertEquals(GoalUnit.PUBLISH, GoalUnit.fromCode("publish"));
        assertEquals(GoalUnit.CONSERVE, GoalUnit.fromCode("conserve"));
        assertEquals(GoalUnit.DISC, GoalUnit.fromCode("disc"));
        assertEquals(GoalUnit.VESSEL, GoalUnit.fromCode("vessel"));
        assertEquals(GoalUnit.ZONE, GoalUnit.fromCode("zone"));
        assertEquals(GoalUnit.FIELD_SYMBOL, GoalUnit.fromCode("fieldSymbol"));
        assertEquals(GoalUnit.IMPACT_MARKER, GoalUnit.fromCode("impactMarker"));
    }

    @Test
    void refuseUnLibelleInconnu() {
        assertThrows(IllegalArgumentException.class, () -> GoalUnit.fromCode("gold"));
    }
}
