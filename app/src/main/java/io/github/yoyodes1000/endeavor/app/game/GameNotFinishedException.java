package io.github.yoyodes1000.endeavor.app.game;

/** Le décompte final n'existe qu'une fois la partie terminée. */
public class GameNotFinishedException extends RuntimeException {

    public GameNotFinishedException() {
        super("La partie n'est pas terminée");
    }
}
