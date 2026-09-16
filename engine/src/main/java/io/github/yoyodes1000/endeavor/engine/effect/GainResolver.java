package io.github.yoyodes1000.endeavor.engine.effect;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.ArrayList;
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
 * <p>{@code lowestAttribute} avance lui aussi une piste sans décision <strong>si
 * une seule</strong> est au plus bas niveau — sinon (égalité) le choix reste en
 * attente, comme {@code anyAttribute}, qui n'est jamais résolu ici (les 4 pistes
 * sont toujours valides).
 *
 * <p>Il ne <strong>pose</strong> rien : la mise en jeu de ces impacts,
 * promotions et choix d'attribut demande une décision et une cascade, laissées
 * à l'appelant.
 */
public final class GainResolver {

    private static final int INGENUITY_VESSEL_LOW = 2;
    private static final int INGENUITY_VESSEL_HIGH = 7;
    private static final int IMPACT_CELL = 10;

    private GainResolver() {
    }

    /**
     * Applique les gains dans l'ordre et renvoie les effets restant à mettre en
     * jeu (impacts, promotions et choix d'attribut ambigus gagnés), et les
     * submersibles gagnés.
     */
    public static EffectOutcome resolve(Player player, List<Gain> gains) {
        int impacts = 0;
        int vessels = 0;
        int promotions = 0;
        int anyAttributeChoices = 0;
        int lowestAttributeChoices = 0;
        for (Gain gain : gains) {
            switch (gain) {
                case INSPIRATION, COORDINATION, REPUTATION, INGENUITY -> {
                    Advance advance = advanceAttribute(player, Attribute.fromCode(gain.code()));
                    impacts += advance.impacts();
                    vessels += advance.vessels();
                }
                case RESEARCH -> player.gainResearch(1);
                case DISC -> player.gainDiscs(1);
                case IMPACT -> impacts++;
                case PROMOTE -> promotions++;
                case ANY_ATTRIBUTE -> anyAttributeChoices++;
                case LOWEST_ATTRIBUTE -> {
                    List<Attribute> lowest = lowestAttributes(player);
                    if (lowest.size() == 1) {
                        Advance advance = advanceAttribute(player, lowest.get(0));
                        impacts += advance.impacts();
                        vessels += advance.vessels();
                    } else {
                        lowestAttributeChoices++;
                    }
                }
            }
        }
        return new EffectOutcome(impacts, vessels, promotions, anyAttributeChoices, lowestAttributeChoices);
    }

    /**
     * Les pistes d'attribut actuellement au plus bas niveau (une seule, sauf
     * égalité) — public pour que le driver d'activation puisse offrir le même
     * choix quand {@code lowestAttribute} est ambigu (résolution différée).
     */
    public static List<Attribute> lowestAttributes(Player player) {
        int min = Integer.MAX_VALUE;
        for (Attribute attribute : Attribute.values()) {
            min = Math.min(min, player.attributes().step(attribute));
        }
        List<Attribute> lowest = new ArrayList<>();
        for (Attribute attribute : Attribute.values()) {
            if (player.attributes().step(attribute) == min) {
                lowest.add(attribute);
            }
        }
        return lowest;
    }

    /** Avance une piste d'un cran et détecte les franchissements (submersible, impact). */
    private static Advance advanceAttribute(Player player, Attribute attribute) {
        player.attributes().advance(attribute, 1);
        int step = player.attributes().step(attribute);
        int vessels = attribute == Attribute.INGENUITY
                && (step == INGENUITY_VESSEL_LOW || step == INGENUITY_VESSEL_HIGH) ? 1 : 0;
        int impacts = step == IMPACT_CELL ? 1 : 0;
        return new Advance(impacts, vessels);
    }

    private record Advance(int impacts, int vessels) {
    }
}
