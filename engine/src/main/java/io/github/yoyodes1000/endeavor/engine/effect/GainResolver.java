package io.github.yoyodes1000.endeavor.engine.effect;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Résout une liste de gains sur un joueur — le cœur déterministe du moteur
 * d'effets (décision 4).
 *
 * <p>Applique directement les gains sans décision : avancer une piste d'attribut,
 * la recherche, ou gagner un disque. En avançant une piste, il DÉTECTE les
 * franchissements — un submersible aux cases 2 et 7 d'ingéniosité, un pion impact
 * à la case 10 (y compris après un dépassement, qui ramène à la case 10) — et
 * remonte le nombre d'impacts et de submersibles gagnés dans un
 * {@link EffectOutcome}.
 *
 * <p>Il ne <strong>pose</strong> rien : la mise en jeu de ces impacts et
 * submersibles demande une décision et une cascade, laissées à l'appelant.
 */
public final class GainResolver {

    private static final int INGENUITY_VESSEL_LOW = 2;
    private static final int INGENUITY_VESSEL_HIGH = 7;
    private static final int IMPACT_CELL = 10;

    private GainResolver() {
    }

    /**
     * Applique les gains dans l'ordre et renvoie les effets restant à mettre en
     * jeu (impacts et submersibles gagnés).
     */
    public static EffectOutcome resolve(Player player, List<Gain> gains) {
        int impacts = 0;
        int vessels = 0;
        for (Gain gain : gains) {
            switch (gain) {
                case INSPIRATION, COORDINATION, REPUTATION, INGENUITY -> {
                    Attribute attribute = Attribute.fromCode(gain.code());
                    player.attributes().advance(attribute, 1);
                    int step = player.attributes().step(attribute);
                    if (attribute == Attribute.INGENUITY
                            && (step == INGENUITY_VESSEL_LOW || step == INGENUITY_VESSEL_HIGH)) {
                        vessels++;
                    }
                    if (step == IMPACT_CELL) {
                        impacts++;
                    }
                }
                case RESEARCH -> player.gainResearch(1);
                case DISC -> player.gainDiscs(1);
                case IMPACT -> impacts++;
                case ANY_ATTRIBUTE, LOWEST_ATTRIBUTE, PROMOTE ->
                        throw new UnsupportedOperationException(
                                "Résolution du gain « " + gain.code() + " » à venir");
            }
        }
        return new EffectOutcome(impacts, vessels);
    }
}
