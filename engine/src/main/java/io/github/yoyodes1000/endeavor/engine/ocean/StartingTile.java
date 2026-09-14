package io.github.yoyodes1000.endeavor.engine.ocean;

/**
 * Une entrée de mise en place de l'océan : où poser une tuile de départ, et
 * laquelle (décision 8, bloc {@code startingTiles}). Deux formes — une tuile
 * <strong>nommée</strong>, ou un <strong>tirage au hasard</strong> dans la pile
 * d'un niveau (information cachée que l'IA doit pouvoir déterminiser).
 *
 * <p>La colonne est un indice 0-based ; la conversion depuis la lettre A, B… se
 * fait à la frontière du chargeur, le moteur restant numérique.
 */
public sealed interface StartingTile permits StartingTile.Named, StartingTile.Random {

    int depth();

    int col();

    /** Une tuile désignée par son identifiant. */
    record Named(int depth, int col, String tileId) implements StartingTile {
        public Named {
            requireCell(depth, col);
            if (tileId == null || tileId.isBlank()) {
                throw new IllegalArgumentException("Une tuile nommée doit avoir un identifiant");
            }
        }
    }

    /** Un tirage au hasard dans la pile d'un niveau de profondeur. */
    record Random(int depth, int col, int level) implements StartingTile {
        public Random {
            requireCell(depth, col);
            if (level < 1 || level > 5) {
                throw new IllegalArgumentException("Niveau de tirage hors de 1..5 : " + level);
            }
        }
    }

    private static void requireCell(int depth, int col) {
        if (depth < 1 || depth > 5) {
            throw new IllegalArgumentException("Profondeur hors de 1..5 : " + depth);
        }
        if (col < 0) {
            throw new IllegalArgumentException("Colonne négative : " + col);
        }
    }
}
