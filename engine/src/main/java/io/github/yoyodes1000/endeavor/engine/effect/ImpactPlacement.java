package io.github.yoyodes1000.endeavor.engine.effect;

import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.player.Player;

/**
 * Pose un pion impact et résout sa récompense — le maillon récursif de la
 * cascade d'effets.
 *
 * <p>Poser sur un hexagone en occupe la case et applique ses gains au joueur, ce
 * qui peut lui-même faire gagner de nouveaux impacts ou submersibles : c'est
 * l'{@link EffectOutcome} renvoyé, à mettre en jeu à son tour. L'appelant (le
 * driver) boucle ainsi tant qu'il reste des effets à poser.
 */
public final class ImpactPlacement {

    private ImpactPlacement() {
    }

    /**
     * Pose le pion du joueur sur l'hexagone et applique sa récompense.
     *
     * @return les impacts et submersibles nouvellement gagnés par la récompense
     */
    public static EffectOutcome place(MissionBoard board, Player player, ImpactHex hex, int playerIndex) {
        board.place(hex, playerIndex);
        return GainResolver.resolve(player, hex.gains());
    }
}
