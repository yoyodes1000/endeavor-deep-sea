package io.github.yoyodes1000.endeavor.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class GainResolverTest {

    private static Player player() {
        return Player.start(Fixtures.teamLeader(), 5);
    }

    @Test
    void avanceLesPistesDAttribut() {
        Player player = player();
        EffectOutcome outcome = GainResolver.resolve(player,
                List.of(Gain.INSPIRATION, Gain.COORDINATION, Gain.COORDINATION));

        assertEquals(1, player.attributes().step(Attribute.INSPIRATION));
        assertEquals(2, player.attributes().step(Attribute.COORDINATION));
        assertEquals(EffectOutcome.NONE, outcome);
    }

    @Test
    void lIngenuiteEnCase2DonneUnSubmersible() {
        EffectOutcome outcome = GainResolver.resolve(player(), List.of(Gain.INGENUITY, Gain.INGENUITY));
        assertEquals(1, outcome.vesselsEarned());
        assertEquals(0, outcome.impactsEarned());
    }

    @Test
    void lesCases2Et7DonnentChacuneUnSubmersible() {
        Player player = player();
        EffectOutcome outcome = GainResolver.resolve(player, Collections.nCopies(7, Gain.INGENUITY));
        assertEquals(2, outcome.vesselsEarned());
        assertEquals(7, player.attributes().step(Attribute.INGENUITY));
    }

    @Test
    void atteindreLaCase10DonneUnImpact() {
        Player player = player();
        EffectOutcome outcome = GainResolver.resolve(player, Collections.nCopies(10, Gain.COORDINATION));
        assertEquals(1, outcome.impactsEarned());
        assertEquals(10, player.attributes().step(Attribute.COORDINATION));
    }

    @Test
    void leDepassementRedonneUnImpact() {
        Player player = player();
        // 13 crans : case 10 (impact), 11, 12, puis dépassement -> retour case 10 (impact)
        EffectOutcome outcome = GainResolver.resolve(player, Collections.nCopies(13, Gain.REPUTATION));
        assertEquals(2, outcome.impactsEarned());
        assertEquals(10, player.attributes().step(Attribute.REPUTATION));
    }

    @Test
    void leGainImpactEstCompteDirectement() {
        EffectOutcome outcome = GainResolver.resolve(player(), List.of(Gain.IMPACT, Gain.IMPACT));
        assertEquals(2, outcome.impactsEarned());
    }

    @Test
    void laRechercheEtLeDisque() {
        Player player = player();
        GainResolver.resolve(player, List.of(Gain.RESEARCH, Gain.RESEARCH, Gain.DISC));
        assertEquals(2, player.research());
        assertEquals(6, player.reserveDiscs()); // 5 de départ + 1 disque gagné
    }

    @Test
    void laRecherchePlafonneADouze() {
        Player player = player();
        GainResolver.resolve(player, Collections.nCopies(15, Gain.RESEARCH));
        assertEquals(12, player.research());
    }

    @Test
    void lePromoteEstCompteDirectement() {
        EffectOutcome outcome = GainResolver.resolve(player(), List.of(Gain.PROMOTE, Gain.PROMOTE));
        assertEquals(2, outcome.promotionsEarned());
    }

    @Test
    void lesGainsDeChoixDAttributNeSontPasEncoreResolus() {
        assertThrows(UnsupportedOperationException.class,
                () -> GainResolver.resolve(player(), List.of(Gain.ANY_ATTRIBUTE)));
        assertThrows(UnsupportedOperationException.class,
                () -> GainResolver.resolve(player(), List.of(Gain.LOWEST_ATTRIBUTE)));
    }
}
