package io.github.yoyodes1000.endeavor.engine.dive;

import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;
import java.util.OptionalInt;

/**
 * Une des alternatives d'un {@link DiveToken} : on en choisit <strong>une</strong>
 * à la dépense, jamais un mélange. Deux formes — <strong>gains</strong> (avec un
 * coût optionnel à payer) ou <strong>action</strong> accordée — à l'image de la
 * règle d'or gain/action déjà appliquée aux tuiles Océan, mais ici encodée par
 * options typées plutôt que par la forme plate des spécialistes : un même jeton
 * peut proposer un choix qui traverse gain ↔ action (ex. « 3 recherche OU un
 * sonar »), ce que {@code ActionSlot} (qui ne mêle que des actions) ne permet pas.
 *
 * <p>Interface scellée : le traitement des deux cas est exhaustif et vérifié par
 * le compilateur, à l'image de {@link io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot}.
 */
public sealed interface DiveOption permits DiveOption.Gains, DiveOption.TriggersAction {

    /**
     * Un lot de gains, éventuellement payé par un coût (ex. « défausser un disque
     * → 5 recherche »).
     *
     * @param gains les gains encaissés
     * @param cost  ce qu'on paie pour l'option, vide si gratuite
     */
    record Gains(List<Gain> gains, List<Gain> cost) implements DiveOption {
        public Gains {
            gains = List.copyOf(gains);
            cost = List.copyOf(cost);
        }
    }

    /**
     * Une action accordée gratuitement (ex. un Sonar sans dépense de disque de
     * transit pour l'activation — l'action garde son propre coût, s'il en a un).
     *
     * @param type          l'action accordée
     * @param costModifier  modificateur du coût de l'action (négatif = réduction),
     *                      vide si l'action n'a pas de coût modifiable
     */
    record TriggersAction(ActionType type, OptionalInt costModifier) implements DiveOption {
        public TriggersAction {
            if (type == null) {
                throw new IllegalArgumentException("Une option d'action doit préciser son type");
            }
            if (costModifier == null) {
                throw new IllegalArgumentException("Le modificateur de coût ne peut être nul (Optional vide sinon)");
            }
        }
    }
}
