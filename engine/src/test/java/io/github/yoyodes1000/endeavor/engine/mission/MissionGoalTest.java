package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MissionGoalTest {

    private static MissionGoal.Standard goal(List<GoalUnit> units, List<GoalUnit> zoneContains) {
        return new MissionGoal.Standard(1, units, List.of(), List.of(), zoneContains, 1,
                Optional.of(new MajorityBonus(5, 3)), "texte");
    }

    @Test
    void refuseUneUniteVide() {
        assertThrows(IllegalArgumentException.class, () -> goal(List.of(), List.of()));
    }

    @Test
    void zoneNeSeCombinePasAvecUneAutreUnite() {
        assertThrows(IllegalArgumentException.class,
                () -> goal(List.of(GoalUnit.ZONE, GoalUnit.DISC), List.of()));
    }

    @Test
    void zoneContainsSupposeLUniteZone() {
        assertThrows(IllegalArgumentException.class,
                () -> goal(List.of(GoalUnit.SONAR), List.of(GoalUnit.DISC)));
    }

    @Test
    void refusePointsParUniteInvalides() {
        assertThrows(IllegalArgumentException.class,
                () -> new MissionGoal.Standard(1, List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(), 0,
                        Optional.empty(), "texte"));
    }

    @Test
    void refuseUnTexteVide() {
        assertThrows(IllegalArgumentException.class,
                () -> new MissionGoal.Standard(1, List.of(GoalUnit.SONAR), List.of(), List.of(), List.of(), 1,
                        Optional.empty(), " "));
    }

    @Test
    void accepteLaFormeStandardOrdinaire() {
        MissionGoal.Standard standard = goal(List.of(GoalUnit.SONAR), List.of());
        assertEquals(1, standard.number());
        assertEquals(List.of(GoalUnit.SONAR), standard.units());
    }

    @Test
    void unObjectifNonSupporteToleresUnTexteVide() {
        MissionGoal.Unsupported unsupported = new MissionGoal.Unsupported(1, null);
        assertEquals("", unsupported.text());
        assertTrue(unsupported instanceof MissionGoal);
    }

    @Test
    void refuseUnNumeroInvalide() {
        assertThrows(IllegalArgumentException.class, () -> new MissionGoal.Unsupported(0, "texte"));
    }
}
