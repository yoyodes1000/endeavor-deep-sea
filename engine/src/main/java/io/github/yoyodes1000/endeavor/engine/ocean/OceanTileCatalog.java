package io.github.yoyodes1000.endeavor.engine.ocean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * L'ensemble des tuiles Océan du matériel. Le constructeur garantit l'unicité des
 * identifiants — comme pour le reste du modèle, une incohérence fait échouer le
 * chargement.
 */
public record OceanTileCatalog(List<OceanTile> tiles) {

    public OceanTileCatalog {
        if (tiles == null || tiles.isEmpty()) {
            throw new IllegalArgumentException("Le catalogue de tuiles Océan est vide");
        }
        tiles = List.copyOf(tiles);

        Map<String, OceanTile> byId = new LinkedHashMap<>();
        for (OceanTile tile : tiles) {
            if (byId.put(tile.id(), tile) != null) {
                throw new IllegalArgumentException("Identifiant de tuile en double : " + tile.id());
            }
        }
    }

    /** La tuile portant cet identifiant, si elle existe. */
    public Optional<OceanTile> byId(String id) {
        return tiles.stream().filter(tile -> tile.id().equals(id)).findFirst();
    }

    /** Les tuiles d'une profondeur donnée, dans l'ordre du catalogue. */
    public List<OceanTile> ofDepth(int depth) {
        return tiles.stream().filter(tile -> tile.depth() == depth).toList();
    }
}
