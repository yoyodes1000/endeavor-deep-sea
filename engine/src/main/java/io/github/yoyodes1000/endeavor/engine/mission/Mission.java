package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;

/**
 * Une mission : son identité, son plateau Impact et la mise en place de son océan
 * ({@link OceanSetup} : colonnes et tuiles de départ). Les objectifs et les règles
 * spéciales de la fiche ne sont pas encore modélisés (décompte / Phase 2) ; le
 * chargeur les ignore.
 *
 * @param number le numéro de mission (1 à 10)
 */
public record Mission(String id, int number, String name, ImpactBoard impactBoard, OceanSetup oceanSetup) {

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
        if (oceanSetup == null) {
            throw new IllegalArgumentException("La mission " + id + " doit avoir une mise en place d'océan");
        }
    }
}
