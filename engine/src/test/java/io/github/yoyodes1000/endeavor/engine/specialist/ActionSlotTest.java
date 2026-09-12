package io.github.yoyodes1000.endeavor.engine.specialist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActionSlotTest {

    @Test
    void refuseUnEmplacementSansChoix() {
        assertThrows(IllegalArgumentException.class, () -> new ActionSlot(List.of()));
    }

    @Test
    void refuseUnEmplacementNul() {
        assertThrows(IllegalArgumentException.class, () -> new ActionSlot(null));
    }

    @Test
    void conserveLesChoixEtLesRendImmuables() {
        List<ActionType> choix = new ArrayList<>(List.of(ActionType.SONAR, ActionType.PUBLISH));
        ActionSlot slot = new ActionSlot(choix);

        choix.clear(); // la copie défensive isole le record de l'appelant
        assertEquals(List.of(ActionType.SONAR, ActionType.PUBLISH), slot.choices());
        assertThrows(UnsupportedOperationException.class, () -> slot.choices().add(ActionType.DIVE));
    }
}
