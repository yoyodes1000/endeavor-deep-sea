package io.github.yoyodes1000.endeavor.app.game;

/** Le coup proposé ne figure pas parmi les coups légaux du joueur à cet instant. */
public class IllegalMoveException extends RuntimeException {

    public IllegalMoveException() {
        super("Coup illégal");
    }
}
