package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de l'étape 1a : recruter la tuile spécialiste d'identifiant donné, prise
 * dans le casier partagé.
 *
 * @param specialistId l'identifiant de la tuile visée
 */
public record Recruter(String specialistId) implements Action {

    public Recruter {
        if (specialistId == null || specialistId.isBlank()) {
            throw new IllegalArgumentException("Un recrutement vise un identifiant de spécialiste");
        }
    }
}
