package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Le coup Promotion : retourner la tuile Junior d'identifiant {@code specialistId}
 * côté Senior — le disque posé dessus est perdu, la nouvelle case d'activation
 * Senior repart libre — puis encaisser les gains immédiats imprimés côté Senior.
 *
 * <p>Résout aussi bien un gain {@code promote} en attente (encaissé au fil d'une
 * autre résolution — site, revue, option de jeton) que l'emplacement d'action
 * dédié d'une chaîne de spécialiste ou une action accordée par un jeton.
 *
 * @param specialistId l'identifiant de la tuile Junior détenue à promouvoir
 */
public record Promouvoir(String specialistId) implements Action {

    public Promouvoir {
        if (specialistId == null || specialistId.isBlank()) {
            throw new IllegalArgumentException("Une Promotion vise une tuile Junior identifiée");
        }
    }
}
