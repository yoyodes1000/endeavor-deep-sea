package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import java.util.List;

/**
 * Ce que l'API montre de la partie au joueur humain. Vue minimale pour l'instant : où
 * en est la partie, à qui c'est le tour, et — si c'est le sien — ses coups légaux.
 * L'état détaillé (océan, plateaux, marché des revues) viendra avec l'écran de jeu.
 *
 * <p>La graine n'y figure pas : elle détermine toutes les piles face cachée.
 *
 * @param mission       le numéro de la mission jouée
 * @param moveNumber    le nombre de coups joués depuis la mise en place, à renvoyer avec
 *                      le prochain coup pour écarter un coup choisi sur un état périmé
 * @param round         la manche en cours (1 à 6)
 * @param phase         la phase en cours
 * @param playerCount   le nombre de joueurs, joueur humain compris
 * @param yourSeat      le siège du joueur humain
 * @param currentPlayer le siège qui doit jouer, {@code null} une fois la partie finie
 * @param yourTurn      vrai si c'est au joueur humain de jouer
 * @param legalActions  ses coups légaux, vide si ce n'est pas son tour
 * @param botMoves      les coups joués par les bots depuis le dernier coup du joueur humain
 *                      (ou depuis la mise en place)
 */
public record GameView(int mission, int moveNumber, int round, GamePhase phase, int playerCount, int yourSeat,
                       Integer currentPlayer, boolean yourTurn, List<Action> legalActions,
                       List<PlayedMove> botMoves) {

    public GameView {
        legalActions = List.copyOf(legalActions);
        botMoves = List.copyOf(botMoves);
    }
}
