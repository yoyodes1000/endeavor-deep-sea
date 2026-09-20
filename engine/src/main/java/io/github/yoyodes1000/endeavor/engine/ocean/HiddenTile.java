package io.github.yoyodes1000.endeavor.engine.ocean;

/**
 * Une tuile cachée : glissée dans la pioche de découverte de son niveau à la mise en
 * place (alors qu'une tuile unique n'y figure jamais). Elle se découvre comme les
 * autres, au hasard.
 *
 * @param tileId l'identifiant de la tuile
 * @param level  le niveau de profondeur de sa pioche
 */
public record HiddenTile(String tileId, int level) {

    public HiddenTile {
        if (tileId == null || tileId.isBlank()) {
            throw new IllegalArgumentException("Une tuile cachée doit avoir un identifiant");
        }
        if (level < 1 || level > OceanSetup.DEEPEST) {
            throw new IllegalArgumentException("Niveau de tuile cachée hors de 1..5 : " + level);
        }
    }
}
