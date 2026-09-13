package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de la Phase 2 : activer un spécialiste en posant un disque de la zone de
 * transit sur sa case d'activation libre.
 *
 * <p>Un seul spécialiste s'active par tour. Exécuter ensuite ses actions est
 * permis mais optionnel : activer « à vide » est un coup légal en soi.
 *
 * @param specialistId l'identifiant de la tuile à activer
 */
public record Activer(String specialistId) implements Action {

    public Activer {
        if (specialistId == null || specialistId.isBlank()) {
            throw new IllegalArgumentException("Une activation vise un identifiant de spécialiste");
        }
    }
}
