package io.github.yoyodes1000.endeavor.engine.mission;

import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
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
 * @param baseTile            ou la tuile qui fait office de base d'opérations (sa case se lit sur
 *                            l'océan bâti), exclusive avec la case
 * @param startingVessels     submersibles de départ par joueur (0 si non relevé)
 * @param goals               les trois objectifs de fin de mission de la fiche
 */
public record Mission(String id, int number, String name, ImpactBoard impactBoard, OceanSetup oceanSetup,
                      Optional<Cell> baseOfOperations, Optional<String> baseTile, int startingVessels,
                      List<MissionGoal> goals) {

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
        if (baseTile == null || (baseOfOperations.isPresent() && baseTile.isPresent())) {
            throw new IllegalArgumentException(
                    "La base d'opérations de " + id + " est une case ou une tuile, pas les deux");
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

    /** Une mission dont la base d'opérations, si elle existe, est une case. */
    public Mission(String id, int number, String name, ImpactBoard impactBoard, OceanSetup oceanSetup,
                   Optional<Cell> baseOfOperations, int startingVessels, List<MissionGoal> goals) {
        this(id, number, name, impactBoard, oceanSetup, baseOfOperations, Optional.empty(), startingVessels, goals);
    }

    /**
     * La zone de lancement de la mission sur l'océan bâti : la case de la base d'opérations, ou la
     * case qui porte sa tuile (la sea-star d'une mission à lignes mélangées n'a pas de case connue
     * d'avance). Vide si la fiche ne désigne pas de zone de lancement.
     */
    public Optional<Cell> launchCell(OceanBoard ocean) {
        if (baseOfOperations.isPresent()) {
            return baseOfOperations;
        }
        return baseTile.flatMap(tileId -> ocean.occupiedCells().stream()
                .filter(cell -> ocean.tileAt(cell).equals(Optional.of(tileId)))
                .findFirst());
    }
}
