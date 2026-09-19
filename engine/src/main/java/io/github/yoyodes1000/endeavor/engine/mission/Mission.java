package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;

import java.util.List;
import java.util.Optional;

/**
 * Une mission : son identité, son plateau Impact et la mise en place de son océan
 * ({@link OceanSetup} : colonnes et tuiles de départ), plus — quand la fiche est
 * relevée — sa <strong>base d'opérations</strong> (où chaque joueur pose son
 * premier submersible), le nombre de submersibles de départ, et ses trois
 * objectifs de fin de mission ({@link MissionGoal}). Les règles spéciales ne
 * sont pas encore modélisées (décompte / Phase 2).
 *
 * @param number              le numéro de mission (1 à 10)
 * @param baseOfOperations    la case de la base d'opérations, si la fiche la déclare
 * @param startingVessels     submersibles de départ par joueur (0 si non relevé)
 * @param goals               les trois objectifs de fin de mission de la fiche
 */
public record Mission(String id, int number, String name, ImpactBoard impactBoard, OceanSetup oceanSetup,
                      Optional<Cell> baseOfOperations, int startingVessels, List<MissionGoal> goals) {

    private static final int GOAL_COUNT = 3;

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
        if (baseOfOperations == null) {
            throw new IllegalArgumentException("La base d'opérations de " + id + " ne peut être nulle (Optional attendu)");
        }
        if (startingVessels < 0) {
            throw new IllegalArgumentException("Nombre de submersibles de départ négatif pour " + id);
        }
        if (goals == null || goals.size() != GOAL_COUNT) {
            throw new IllegalArgumentException(
                    "La mission " + id + " doit avoir exactement " + GOAL_COUNT + " objectifs");
        }
        goals = List.copyOf(goals);
    }
}
