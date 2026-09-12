package io.github.yoyodes1000.endeavor.engine.preparation;

import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;

import java.util.ArrayList;
import java.util.List;

/**
 * L'étape de Recrutement (1a) : le joueur choisit une tuile du casier partagé,
 * dans la limite de son niveau de réputation.
 *
 * <p>Cette classe ne couvre que la <strong>mécanique</strong> du recrutement —
 * quelles tuiles sont légales, et le déplacement du casier vers le joueur. La
 * résolution des gains de la tuile (le « petit moteur d'effets ») est traitée à
 * part, et suivra.
 */
public final class Recruitment {

    private Recruitment() {
    }

    /** Les recrutements légaux d'un joueur : les tuiles du casier de rang ≤ sa réputation. */
    public static List<Recruter> legalRecruits(GameState state, int playerIndex) {
        int reputation = state.player(playerIndex).attributes().level(Attribute.REPUTATION);
        List<Recruter> recruits = new ArrayList<>();
        for (Specialist specialist : state.casier()) {
            if (specialist.rank().orElseThrow() <= reputation) {
                recruits.add(new Recruter(specialist.id()));
            }
        }
        return recruits;
    }

    /**
     * Applique un recrutement : retire la tuile du casier et la donne au joueur,
     * face Junior. Mutation en place.
     *
     * @throws IllegalArgumentException si le recrutement n'est pas légal (tuile
     *     absente du casier, ou de rang supérieur à la réputation du joueur)
     */
    public static void applyRecruit(GameState state, int playerIndex, Recruter action) {
        if (!legalRecruits(state, playerIndex).contains(action)) {
            throw new IllegalArgumentException("Recrutement illégal : " + action.specialistId());
        }
        Specialist specialist = state.removeFromCasier(action.specialistId());
        state.player(playerIndex).recruit(HeldSpecialist.recruited(specialist));
    }
}
