package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MissionLaunchCellTest {

    private static final MissionGoal GOAL = new MissionGoal.Standard(1, List.of(GoalUnit.DISC), List.of(), List.of(),
            List.of(), 1, Optional.empty(), "t");

    private static Mission mission(Optional<Cell> cell, Optional<String> tile) {
        return new Mission("m", 1, "M", Fixtures.missionBoard().board(), new OceanSetup(3, List.of()), cell, tile, 1,
                List.of(GOAL, GOAL, GOAL));
    }

    private static OceanBoard ocean() {
        OceanBoard ocean = new OceanBoard(3);
        ocean.placeTile(new Cell(1, 0), "site");
        ocean.placeTile(new Cell(1, 2), "the-sea-star");
        return ocean;
    }

    @Test
    void uneBaseDonneeParCaseEstLaZoneDeLancement() {
        assertEquals(Optional.of(new Cell(1, 0)),
                mission(Optional.of(new Cell(1, 0)), Optional.empty()).launchCell(ocean()));
    }

    @Test
    void uneBaseDonneeParTuileSeLitSurLOceanBati() {
        assertEquals(Optional.of(new Cell(1, 2)),
                mission(Optional.empty(), Optional.of("the-sea-star")).launchCell(ocean()));
    }

    @Test
    void sansBaseLaMissionNAPasDeZoneDeLancement() {
        assertEquals(Optional.empty(), mission(Optional.empty(), Optional.empty()).launchCell(ocean()));
        assertEquals(Optional.empty(), mission(Optional.empty(), Optional.of("absente")).launchCell(ocean()));
    }

    @Test
    void laBaseEstUneCaseOuUneTuilePasLesDeux() {
        assertThrows(IllegalArgumentException.class,
                () -> mission(Optional.of(new Cell(1, 0)), Optional.of("the-sea-star")));
    }
}
