package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Le coup Dépense d'un jeton de plongée : choisir l'une des options du jeton en
 * main désigné par {@code heldIndex} — jamais un mélange. Libre en nombre, en
 * plus (ou à la place) de l'activation d'un spécialiste ; peut être joué à
 * n'importe quel point de décision normal du tour (l'{@code ActivationDriver}
 * l'offre partout où il offre {@code TerminerTour}/{@code Passer}).
 *
 * <p>Si l'option choisie est un lot de gains, elle est résolue entièrement par ce
 * seul coup. Si c'est une action accordée (Sonar, Voyage, Plongée), le tour se
 * suspend sur cette seule action — hors chaîne du spécialiste — jusqu'à ce
 * qu'elle soit jouée.
 *
 * @param heldIndex   l'index du jeton en main (0-based, ordre de prise)
 * @param optionIndex l'index de l'option choisie sur ce jeton (0-based)
 */
public record DepenserJeton(int heldIndex, int optionIndex) implements Action {

    public DepenserJeton {
        if (heldIndex < 0) {
            throw new IllegalArgumentException("Index de jeton en main négatif : " + heldIndex);
        }
        if (optionIndex < 0) {
            throw new IllegalArgumentException("Index d'option négatif : " + optionIndex);
        }
    }
}
