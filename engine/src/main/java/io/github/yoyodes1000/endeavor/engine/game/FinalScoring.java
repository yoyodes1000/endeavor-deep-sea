package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.player.HeldSpecialist;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameScoring;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;

import java.util.ArrayList;
import java.util.List;

/**
 * Le décompte final : assemble les trois sources de points (attributs et revues,
 * tuiles Senior de rang 5, objectifs de mission) et désigne le ou les vainqueurs.
 *
 * <p>Ce qui n'est pas encore modélisé (objectif {@link MissionGoal.Unsupported},
 * unité ou effectif non câblé) ne fait pas échouer la fin de partie : il compte
 * pour zéro et est signalé dans {@link FinalResult#uncounted()}.
 */
public final class FinalScoring {

    private FinalScoring() {
    }

    public static FinalResult compute(GameState state) {
        List<String> uncounted = new ArrayList<>();
        List<FinalResult.PlayerScore> scores = new ArrayList<>();
        for (int playerIndex = 0; playerIndex < state.playerCount(); playerIndex++) {
            scores.add(new FinalResult.PlayerScore(
                    state.player(playerIndex).finalScore(state.journalCatalog()),
                    seniorPoints(state, playerIndex, uncounted),
                    goalPoints(state, playerIndex, uncounted)));
        }
        return new FinalResult(scores, winners(scores), uncounted.stream().distinct().toList());
    }

    private static int seniorPoints(GameState state, int playerIndex, List<String> uncounted) {
        int total = 0;
        for (HeldSpecialist held : state.player(playerIndex).specialists()) {
            if (held.face() != SpecialistFace.SENIOR || held.activeSide().endGameScoring().isEmpty()) {
                continue;
            }
            EndGameScoring scoring = held.activeSide().endGameScoring().get();
            try {
                total += scoring.score(
                        EndGameCounter.effectif(scoring.count(), state, playerIndex, held.specialist().id()));
            } catch (UnsupportedOperationException notWired) {
                uncounted.add("Senior " + held.specialist().id() + " : " + notWired.getMessage());
            }
        }
        return total;
    }

    private static int goalPoints(GameState state, int playerIndex, List<String> uncounted) {
        int total = 0;
        for (MissionGoal goal : state.missionBoard().goals()) {
            switch (goal) {
                case MissionGoal.Standard standard ->
                        total += standardGoalPoints(standard, state, playerIndex, uncounted);
                case MissionGoal.Unsupported unsupported ->
                        uncounted.add("Objectif " + unsupported.number() + " : forme non modélisée");
            }
        }
        return total;
    }

    private static int standardGoalPoints(MissionGoal.Standard goal, GameState state, int playerIndex,
                                          List<String> uncounted) {
        try {
            return MissionGoalScorer.score(goal, state, playerIndex);
        } catch (UnsupportedOperationException notWired) {
            uncounted.add("Objectif " + goal.number() + " : " + notWired.getMessage());
            return 0;
        }
    }

    private static List<Integer> winners(List<FinalResult.PlayerScore> scores) {
        int best = scores.stream().mapToInt(FinalResult.PlayerScore::total).max().orElseThrow();
        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).total() == best) {
                winners.add(i);
            }
        }
        return winners;
    }
}
