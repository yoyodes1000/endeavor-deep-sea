package io.github.yoyodes1000.endeavor.engine.journal;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Une revue scientifique du matériel : son coût de publication, ses points de
 * fin de partie, les symboles de domaine des sites où elle peut être publiée,
 * et les gains qu'elle encaisse — pour le joueur qui la publie et pour ses
 * adversaires.
 *
 * <p>Le champ {@code actions} du matériel (certaines revues acquises
 * deviennent un site activable : poser un disque dessus pour déclencher une
 * action, disque jamais repris) n'est <strong>pas</strong> modélisé — c'est une
 * mécanique à part entière, distincte de la Publication elle-même, qui relève
 * d'une carte sœur (dans la continuité du trou {@code arrivalActions} laissé
 * par {@link io.github.yoyodes1000.endeavor.engine.ocean.OceanTile}). Le
 * chargeur tolère ce champ sans le lire.
 *
 * @param id              l'identifiant de la revue
 * @param anchor          revue de départ (8 au total, dont 4 forment le marché
 *                        initial) par opposition à une revue standard (24, en pioche)
 * @param name             le nom de la revue
 * @param researchCost     le coût en recherche à payer pour la publier
 * @param victoryPoints    les points imprimés, comptés au décompte final
 * @param fieldSymbols     les symboles de domaine où elle peut être publiée, au moins un
 * @param publisherGains   les gains encaissés par le joueur qui publie
 * @param opponentsGains   les gains encaissés par chacun des autres joueurs
 */
public record Journal(
        String id,
        boolean anchor,
        String name,
        int researchCost,
        int victoryPoints,
        List<FieldSymbol> fieldSymbols,
        List<Gain> publisherGains,
        List<Gain> opponentsGains) {

    public Journal {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Une revue doit avoir un identifiant");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("La revue " + id + " doit avoir un nom");
        }
        if (researchCost < 0) {
            throw new IllegalArgumentException("Coût de recherche négatif pour " + id + " : " + researchCost);
        }
        if (victoryPoints < 0) {
            throw new IllegalArgumentException("Points de victoire négatifs pour " + id + " : " + victoryPoints);
        }
        if (fieldSymbols == null || fieldSymbols.isEmpty()) {
            throw new IllegalArgumentException("La revue " + id + " doit porter au moins un symbole de domaine");
        }
        fieldSymbols = List.copyOf(fieldSymbols);
        publisherGains = List.copyOf(publisherGains);
        opponentsGains = List.copyOf(opponentsGains);
    }
}
