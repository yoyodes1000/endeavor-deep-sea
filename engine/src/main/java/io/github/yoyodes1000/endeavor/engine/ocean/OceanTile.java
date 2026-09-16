package io.github.yoyodes1000.endeavor.engine.ocean;

import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.util.List;

/**
 * Une tuile Océan : son identité, ses gains, et sa première structure
 * d'activation — les <strong>pistes Sonar</strong>, sur lesquelles l'action
 * Sonar pose un disque.
 *
 * <p>Le reste de la structure d'activation (conservation, revues, connexions,
 * règles spéciales) n'est <strong>pas</strong> encore modélisé : il relève de la
 * Phase 2 et sera conçu sur des cas réels (cf. architecture, points ouverts). Le
 * chargeur tolère ces champs sans les lire.
 *
 * @param depth          la profondeur, de 1 à 5
 * @param unique         tuile de scénario, hors pioche de découverte
 * @param discoverBonus  gains à la découverte (pose de la tuile)
 * @param arrivalBonus   gains quand un submersible arrive sur la tuile
 * @param arrivalActions actions accordées à l'arrivée
 * @param sonarTracks    les pistes Sonar de la tuile (souvent aucune)
 * @param diveSites      les sites de plongée de la tuile (souvent aucun)
 */
public record OceanTile(
        String id,
        String name,
        int depth,
        boolean unique,
        List<Gain> discoverBonus,
        List<Gain> arrivalBonus,
        List<ActionType> arrivalActions,
        List<SonarTrack> sonarTracks,
        List<DiveSite> diveSites) {

    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 5;

    public OceanTile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Une tuile Océan doit avoir un identifiant");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("La tuile " + id + " doit avoir un nom");
        }
        if (depth < MIN_DEPTH || depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "Profondeur hors de " + MIN_DEPTH + ".." + MAX_DEPTH + " pour " + id + " : " + depth);
        }
        discoverBonus = List.copyOf(discoverBonus);
        arrivalBonus = List.copyOf(arrivalBonus);
        arrivalActions = List.copyOf(arrivalActions);
        sonarTracks = List.copyOf(sonarTracks);
        diveSites = List.copyOf(diveSites);
    }
}
