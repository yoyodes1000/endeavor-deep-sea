package io.github.yoyodes1000.endeavor.engine.ocean;

/**
 * Une case de la grille de l'océan : une <strong>profondeur</strong> (1 à 5, la
 * surface étant 1) et une <strong>colonne</strong> (indice 0-based dans le
 * moteur ; la lettre A, B… n'existe que dans les données, convertie à la
 * frontière du chargeur).
 *
 * <p>Coordonnée pure et immuable. Ce qui occupe la case — la tuile, les
 * submersibles — vit dans {@link OceanBoard}. Le {@code record} fournit
 * {@code equals}/{@code hashCode}, ce qui rend {@code Cell} utilisable directement
 * comme clé de dictionnaire.
 */
public record Cell(int depth, int col) {

    public Cell {
        if (depth < 1) {
            throw new IllegalArgumentException("Profondeur invalide : " + depth);
        }
        if (col < 0) {
            throw new IllegalArgumentException("Colonne négative : " + col);
        }
    }
}
