package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Le coup Choix d'option : le joueur qui vient de poser un pion sur un hexagone
 * objectif choisit l'option de cet objectif. Le choix vaut pour tous les joueurs.
 *
 * @param optionId l'identifiant de l'option retenue
 */
public record ChoisirOption(String optionId) implements Action {

    public ChoisirOption {
        if (optionId == null || optionId.isBlank()) {
            throw new IllegalArgumentException("Un choix d'option vise une option identifiée");
        }
    }
}
