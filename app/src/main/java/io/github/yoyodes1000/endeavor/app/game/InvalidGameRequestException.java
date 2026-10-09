package io.github.yoyodes1000.endeavor.app.game;

/** Une demande de partie ou de coup incomplète ou hors bornes. */
public class InvalidGameRequestException extends RuntimeException {

    public InvalidGameRequestException(String message) {
        super(message);
    }
}
