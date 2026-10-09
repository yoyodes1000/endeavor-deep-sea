package io.github.yoyodes1000.endeavor.ai;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.game.PlayerView;

import java.util.List;

/**
 * Le bot le plus simple : un coup légal tiré au hasard. Il ne regarde pas la partie,
 * d'où une vue ignorée. Il sert d'adversaire de démonstration en attendant la
 * recherche Monte-Carlo.
 *
 * <p>Son aléa est injecté, comme celui du moteur : à graine égale, il rejoue les
 * mêmes choix.
 */
public final class RandomBot implements Bot {

    private final RandomSource random;

    public RandomBot(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("Le bot aléatoire a besoin d'une source d'aléa");
        }
        this.random = random;
    }

    @Override
    public Action choose(PlayerView view, List<Action> legalActions) {
        if (legalActions.isEmpty()) {
            throw new IllegalArgumentException("Aucun coup légal à choisir");
        }
        return legalActions.get(random.nextInt(legalActions.size()));
    }
}
