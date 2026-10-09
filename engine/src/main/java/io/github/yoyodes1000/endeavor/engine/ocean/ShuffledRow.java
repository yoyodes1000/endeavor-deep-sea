package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.List;

/**
 * Une ligne de départ dont les tuiles sont <strong>mélangées</strong> entre ses colonnes
 * (information cachée : la mise en place tire l'ordre). Chaque entrée est une tuile
 * nommée ou un tirage dans la pile d'un niveau ; il y en a autant que de colonnes.
 *
 * @param depth   la profondeur de la ligne
 * @param columns les colonnes de la ligne (indices 0-based), distinctes
 * @param entries les tuiles à répartir dans ces colonnes, une par colonne
 */
public record ShuffledRow(int depth, List<Integer> columns, List<Entry> entries) {

    /** Ce qui se pose dans une colonne de la ligne : une tuile nommée ou un tirage. */
    public sealed interface Entry permits Named, Random {
    }

    public record Named(String tileId) implements Entry {
        public Named {
            if (tileId == null || tileId.isBlank()) {
                throw new IllegalArgumentException("Une tuile nommée doit avoir un identifiant");
            }
        }
    }

    public record Random(int level) implements Entry {
        public Random {
            if (level < 1 || level > OceanSetup.DEEPEST) {
                throw new IllegalArgumentException("Niveau de tirage hors de 1..5 : " + level);
            }
        }
    }

    public ShuffledRow {
        if (depth < 1 || depth > OceanSetup.DEEPEST) {
            throw new IllegalArgumentException("Profondeur hors de 1..5 : " + depth);
        }
        if (columns == null || columns.isEmpty() || columns.stream().anyMatch(col -> col < 0)
                || columns.stream().distinct().count() != columns.size()) {
            throw new IllegalArgumentException("Une ligne mélangée a des colonnes distinctes et positives");
        }
        if (entries == null || entries.size() != columns.size()) {
            throw new IllegalArgumentException("Une ligne mélangée a autant de tuiles que de colonnes");
        }
        columns = List.copyOf(columns);
        entries = List.copyOf(entries);
    }
}
