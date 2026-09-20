package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * Une option d'un objectif à options ({@link MissionGoal.Choice}) : un identifiant
 * et l'objectif standard que cette option calcule.
 */
public record GoalOption(String id, MissionGoal.Standard goal) {

    public GoalOption {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Une option d'objectif doit avoir un identifiant");
        }
        if (goal == null) {
            throw new IllegalArgumentException("L'option " + id + " doit décrire son calcul");
        }
    }
}
