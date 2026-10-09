package io.github.yoyodes1000.endeavor.app.game;

/**
 * Le coup a été choisi sur un état périmé : la partie a avancé depuis (un autre
 * onglet, une requête rejouée). Le client doit relire l'état avant de rejouer.
 */
public class StaleMoveException extends RuntimeException {

    public StaleMoveException() {
        super("La partie a avancé depuis ce coup");
    }
}
