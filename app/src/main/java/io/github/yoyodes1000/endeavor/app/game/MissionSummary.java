package io.github.yoyodes1000.endeavor.app.game;

import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;

/**
 * Une mission proposée au lancement d'une partie.
 *
 * @param number         le numéro de la mission
 * @param name           son nom
 * @param allGoalsScored vrai si le moteur sait compter ses trois objectifs ; sinon, les
 *                       objectifs non câblés rapportent zéro point au décompte
 */
public record MissionSummary(int number, String name, boolean allGoalsScored) {

    static MissionSummary of(Mission mission) {
        boolean allGoalsScored = mission.goals().stream().noneMatch(MissionGoal.Unsupported.class::isInstance);
        return new MissionSummary(mission.number(), mission.name(), allGoalsScored);
    }
}
