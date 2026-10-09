package io.github.yoyodes1000.endeavor.ai;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.game.PlayerView;

import java.util.List;

/**
 * Un joueur artificiel : il choisit un coup parmi les coups légaux, à partir de ce
 * que son siège lui laisse voir de la partie — jamais de l'état complet (décision 2
 * de l'architecture). C'est ce qui l'empêche mécaniquement de lire les piles face
 * cachée.
 */
public interface Bot {

    /**
     * @param view         ce que le bot observe de la partie, depuis son siège
     * @param legalActions les coups légaux du moment, jamais vide
     * @return un coup de la liste
     */
    Action choose(PlayerView view, List<Action> legalActions);
}
