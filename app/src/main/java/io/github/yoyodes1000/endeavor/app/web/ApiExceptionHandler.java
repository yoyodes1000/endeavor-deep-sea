package io.github.yoyodes1000.endeavor.app.web;

import io.github.yoyodes1000.endeavor.app.game.GameNotFinishedException;
import io.github.yoyodes1000.endeavor.app.game.IllegalMoveException;
import io.github.yoyodes1000.endeavor.app.game.InvalidGameRequestException;
import io.github.yoyodes1000.endeavor.app.game.NoGameInProgressException;
import io.github.yoyodes1000.endeavor.app.game.StaleMoveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduit les erreurs en réponses « problem+json » (RFC 9457). Les messages sont
 * écrits par l'application et ne reprennent jamais la saisie du client ; une erreur
 * inattendue ne renvoie qu'un message générique, le détail restant dans le journal du
 * serveur.
 *
 * <p>Les erreurs standard de Spring MVC (JSON illisible, type de contenu refusé…) sont
 * traitées par la classe mère.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidGameRequestException.class)
    ProblemDetail invalidRequest(InvalidGameRequestException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(NoGameInProgressException.class)
    ProblemDetail noGame(NoGameInProgressException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler({StaleMoveException.class, GameNotFinishedException.class})
    ProblemDetail conflict(RuntimeException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    @ExceptionHandler(IllegalMoveException.class)
    ProblemDetail illegalMove(IllegalMoveException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception error) {
        LOG.error("Erreur inattendue pendant une requête", error);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne");
    }
}
