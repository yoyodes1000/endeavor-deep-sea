package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * Une mission : son identité et son plateau Impact. La mise en place, les
 * objectifs et les règles spéciales de la fiche ne sont pas encore modélisés
 * (décompte / Phase 2) ; le chargeur les ignore.
 *
 * @param number le numéro de mission (1 à 10)
 */
public record Mission(String id, int number, String name, ImpactBoard impactBoard) {

    public Mission {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Une mission doit avoir un identifiant");
        }
        if (number < 1) {
            throw new IllegalArgumentException("Numéro de mission invalide : " + number);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("La mission " + id + " doit avoir un nom");
        }
        if (impactBoard == null) {
            throw new IllegalArgumentException("La mission " + id + " doit avoir un plateau Impact");
        }
    }
}
