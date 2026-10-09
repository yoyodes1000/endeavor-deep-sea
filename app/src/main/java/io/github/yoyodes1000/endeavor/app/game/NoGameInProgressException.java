package io.github.yoyodes1000.endeavor.app.game;

/** Aucune partie n'a été lancée : il n'y a ni état à montrer ni coup à jouer. */
public class NoGameInProgressException extends RuntimeException {

    public NoGameInProgressException() {
        super("Aucune partie en cours");
    }
}
