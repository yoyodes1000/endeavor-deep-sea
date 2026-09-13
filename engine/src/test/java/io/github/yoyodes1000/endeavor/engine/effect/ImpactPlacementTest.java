package io.github.yoyodes1000.endeavor.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImpactPlacementTest {

    private static MissionBoard boardWithStart(Gain... startGains) {
        ImpactHex start = new ImpactHex(0, 0, 0, List.of(startGains), true, false, false);
        return new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, List.of(start)));
    }

    private static Player player() {
        return Player.start(Fixtures.teamLeader(), 5);
    }

    @Test
    void poserResoutLaRecompense() {
        MissionBoard board = boardWithStart(Gain.REPUTATION);
        Player player = player();
        ImpactHex depart = board.board().hexAt(0, 0).orElseThrow();

        EffectOutcome outcome = ImpactPlacement.place(board, player, depart, 0);

        assertEquals(1, player.attributes().step(Attribute.REPUTATION));
        assertEquals(EffectOutcome.NONE, outcome);
        assertTrue(board.isOccupied(depart));
    }

    @Test
    void poserUnImpactRelanceLaCascade() {
        MissionBoard board = boardWithStart(Gain.IMPACT);
        Player player = player();
        ImpactHex depart = board.board().hexAt(0, 0).orElseThrow();

        EffectOutcome outcome = ImpactPlacement.place(board, player, depart, 0);

        assertEquals(1, outcome.impactsEarned(), "une récompense impact relance la cascade");
    }
}
