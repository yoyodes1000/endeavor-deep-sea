package io.github.yoyodes1000.endeavor.engine.preparation;

import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.GainResolver;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;

import java.util.ArrayList;
import java.util.List;

/**
 * L'étape de Recrutement (1a) : le joueur choisit une tuile du casier partagé,
 * dans la limite de son niveau de réputation, puis en résout les gains immédiats.
 *
 * <p>La résolution des gains est déléguée au {@link GainResolver} : les effets
 * directs (pistes, recherche, disque) sont appliqués, et les impacts / submersibles
 * gagnés sont remontés pour être mis en jeu par l'appelant (la cascade).
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
     * Applique un recrutement : retire la tuile du casier, la donne au joueur
     * (face Junior), puis résout ses gains immédiats. Mutation en place.
     *
     * @return les impacts et submersibles gagnés par les gains de la tuile, à
     *     mettre en jeu à leur tour
     * @throws IllegalArgumentException si le recrutement n'est pas légal (tuile
     *     absente du casier, ou de rang supérieur à la réputation du joueur)
     */
    public static EffectOutcome applyRecruit(GameState state, int playerIndex, Recruter action) {
        if (!legalRecruits(state, playerIndex).contains(action)) {
            throw new IllegalArgumentException("Recrutement illégal : " + action.specialistId());
        }
        Specialist specialist = state.removeFromCasier(action.specialistId());
        Player player = state.player(playerIndex);
        HeldSpecialist held = HeldSpecialist.recruited(specialist);
        player.recruit(held);
        return GainResolver.resolve(player, held.activeSide().immediateGains());
    }
}
