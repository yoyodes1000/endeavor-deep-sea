package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Coup de l'étape 1c (Récupération) : reprendre un disque posé sur la tuile
 * spécialiste d'identifiant donné, vers la zone de transit.
 *
 * <p>Un coup reprend <strong>un</strong> disque : le joueur en enchaîne autant que
 * son niveau de coordination l'autorise, choisissant à chaque fois sur quel
 * spécialiste — c'est là le vrai choix de l'étape.
 *
 * @param specialistId l'identifiant de la tuile dont on reprend un disque
 */
public record Recuperer(String specialistId) implements Action {

    public Recuperer {
        if (specialistId == null || specialistId.isBlank()) {
            throw new IllegalArgumentException("Une récupération vise un identifiant de spécialiste");
        }
    }
}
