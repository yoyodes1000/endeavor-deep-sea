package io.github.yoyodes1000.endeavor.engine.dive;

import java.util.List;

/**
 * Un type de jeton de plongée du matériel : son identifiant, le nombre
 * d'exemplaires physiques, et ses alternatives à la dépense — on en choisit
 * <strong>une</strong> ({@link DiveOption}), jamais un mélange.
 *
 * <p>Matériel immuable, au même titre qu'{@code OceanTile}. Les exemplaires
 * tirés et empilés sur un site sont de l'état mutable, porté par
 * {@link io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard}.
 *
 * @param id      l'identifiant du type de jeton
 * @param copies  le nombre d'exemplaires physiques (pour la pile de tirage)
 * @param options les alternatives à la dépense, au moins une
 */
public record DiveToken(String id, int copies, List<DiveOption> options) {

    public DiveToken {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un jeton de plongée doit avoir un identifiant");
        }
        if (copies < 1) {
            throw new IllegalArgumentException("Un jeton de plongée a au moins un exemplaire : " + copies);
        }
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Le jeton " + id + " doit proposer au moins une option");
        }
        options = List.copyOf(options);
    }
}
