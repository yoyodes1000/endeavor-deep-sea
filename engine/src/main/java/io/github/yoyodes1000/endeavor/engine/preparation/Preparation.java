package io.github.yoyodes1000.endeavor.engine.preparation;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.Player;

/**
 * Les étapes <strong>automatiques</strong> de la phase de préparation : le
 * premier joueur et l'effort.
 *
 * <p>Ce ne sont pas des coups : le joueur n'y décide rien, le moteur les applique
 * seul en avançant jusqu'au prochain point de décision (décision 1). Les étapes
 * de décision (recrutement 1a, récupération 1c) vivront ailleurs.
 */
public final class Preparation {

    private Preparation() {
    }

    /**
     * Fixe le premier joueur de la manche courante.
     *
     * <p>Manche 1 : tirage aléatoire via la graine injectée — jamais d'appel
     * implicite au hasard, pour que la partie reste reproductible. Manches
     * suivantes : le rôle passe d'un cran dans le sens horaire.
     */
    public static void chooseFirstPlayer(GameState state) {
        int first;
        if (state.round() == 1) {
            first = state.random().nextInt(state.playerCount());
        } else {
            first = (state.firstPlayerIndex() + 1) % state.playerCount();
        }
        state.setFirstPlayerIndex(first);
    }

    /**
     * Étape Effort (1b) : le joueur prend, de la réserve vers la zone de transit,
     * autant de disques que son niveau d'inspiration. Aucune décision.
     */
    public static void applyEffort(Player player) {
        int discs = player.attributes().level(Attribute.INSPIRATION);
        player.moveReserveToTransit(discs);
    }
}
