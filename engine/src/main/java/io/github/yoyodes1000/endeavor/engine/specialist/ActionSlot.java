package io.github.yoyodes1000.endeavor.engine.specialist;

import java.util.List;

/**
 * Un emplacement d'action d'une tuile : le joueur y choisit <strong>une</strong>
 * des actions proposées.
 *
 * <p>Une tuile enchaîne plusieurs emplacements (une {@code List<ActionSlot>} du
 * côté). L'encodage des données superpose deux niveaux : le tableau
 * <em>intérieur</em> est le choix (les alternatives d'un même emplacement), le
 * tableau <em>extérieur</em> est l'enchaînement (les emplacements successifs).
 *
 * @param choices les actions au choix pour cet emplacement, au moins une
 */
public record ActionSlot(List<ActionType> choices) {

    public ActionSlot {
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException(
                    "Un emplacement d'action doit proposer au moins un choix");
        }
        choices = List.copyOf(choices);
    }
}
