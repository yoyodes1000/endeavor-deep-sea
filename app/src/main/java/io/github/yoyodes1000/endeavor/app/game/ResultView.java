package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.engine.game.FinalResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Le décompte final, tel que l'API le montre.
 *
 * @param scores    le détail par siège
 * @param winners   les sièges au score maximal (plusieurs en cas d'égalité)
 * @param complete  faux si un élément du décompte n'est pas encore câblé dans le moteur :
 *                  le classement n'est alors pas définitif
 * @param uncounted les éléments non comptés, vide si le décompte est complet
 */
public record ResultView(List<Score> scores, List<Integer> winners, boolean complete, List<String> uncounted) {

    public ResultView {
        scores = List.copyOf(scores);
        winners = List.copyOf(winners);
        uncounted = List.copyOf(uncounted);
    }

    static ResultView of(FinalResult result) {
        List<Score> scores = new ArrayList<>();
        for (int seat = 0; seat < result.scores().size(); seat++) {
            FinalResult.PlayerScore score = result.scores().get(seat);
            scores.add(new Score(seat, score.attributesAndJournals(), score.seniorEndGame(), score.missionGoals(),
                    score.total()));
        }
        return new ResultView(scores, result.winners(), result.isComplete(), result.uncounted());
    }

    /**
     * Le score d'un siège.
     *
     * @param seat                  le siège
     * @param attributesAndJournals les points des attributs et des revues
     * @param seniorEndGame         les points des décomptes Senior de rang 5
     * @param missionGoals          les points des objectifs de mission
     * @param total                 la somme des trois
     */
    public record Score(int seat, int attributesAndJournals, int seniorEndGame, int missionGoals, int total) {
    }
}
