package io.github.yoyodes1000.endeavor.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.game.PlayerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomBotTest {

    /** Le bot aléatoire ne lit pas la partie : aucune vue n'est nécessaire pour l'éprouver. */
    private static final PlayerView UNUSED_VIEW = null;

    private static final List<Action> LEGAL = List.of(new Passer(), new TerminerTour(), new Activer("pilot"));

    private static final int DRAWS = 200;

    private static List<Action> draws(long seed) {
        RandomBot bot = new RandomBot(RandomSource.fromSeed(seed));
        List<Action> chosen = new ArrayList<>();
        for (int i = 0; i < DRAWS; i++) {
            chosen.add(bot.choose(UNUSED_VIEW, LEGAL));
        }
        return chosen;
    }

    @Test
    void choisitToujoursUnCoupLegal() {
        assertTrue(LEGAL.containsAll(draws(1)));
    }

    @Test
    void finitParProposerChacunDesCoups() {
        Set<Action> distinct = new HashSet<>(draws(2));
        assertEquals(Set.copyOf(LEGAL), distinct);
    }

    @Test
    void aGraineEgaleRejoueLesMemesChoix() {
        assertEquals(draws(3), draws(3));
    }

    @Test
    void refuseDeChoisirSansCoupLegal() {
        RandomBot bot = new RandomBot(RandomSource.fromSeed(4));
        assertThrows(IllegalArgumentException.class, () -> bot.choose(UNUSED_VIEW, List.of()));
    }

    @Test
    void exigeUneSourceDAlea() {
        assertThrows(IllegalArgumentException.class, () -> new RandomBot(null));
    }
}
