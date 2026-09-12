package io.github.yoyodes1000.endeavor.app.ocean;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Charge le catalogue des tuiles Océan depuis le JSON du matériel et le traduit
 * vers le modèle du moteur.
 *
 * <p>Même partage que pour les spécialistes : Jackson lit la forme, le moteur
 * valide le vocabulaire ({@code fromCode}) et la sémantique (constructeurs).
 * Chargeur agnostique de l'I/O (il lit un {@link Reader}).
 *
 * <p>Les champs d'activation de la tuile (Phase 2) ne sont pas encore modélisés :
 * le mapper est réglé pour <strong>ignorer</strong> les propriétés inconnues, le
 * temps que ces champs soient conçus sur des cas réels.
 */
public final class OceanTileLoader {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Lit et valide le catalogue.
     *
     * @throws IllegalArgumentException si une donnée est incohérente (gain inconnu,
     *     profondeur invalide, identifiant en double…)
     * @throws UncheckedIOException si le flux est illisible ou mal formé
     */
    public OceanTileCatalog load(Reader source) {
        OceanTileDocument document = read(source);
        if (document == null || document.oceanTiles() == null) {
            throw new IllegalArgumentException("JSON des tuiles vide ou sans tableau « oceanTiles »");
        }
        List<OceanTile> tiles = document.oceanTiles().stream()
                .map(OceanTileLoader::toTile)
                .toList();
        return new OceanTileCatalog(tiles);
    }

    private OceanTileDocument read(Reader source) {
        try {
            return mapper.readValue(source, OceanTileDocument.class);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON des tuiles Océan illisible", e);
        }
    }

    private static OceanTile toTile(OceanTileDocument.Entry entry) {
        if (entry.depth() == null) {
            throw new IllegalArgumentException("Profondeur manquante pour " + entry.id());
        }
        return new OceanTile(
                entry.id(),
                entry.name(),
                entry.depth(),
                Boolean.TRUE.equals(entry.unique()),
                gains(entry.discoverBonus()),
                gains(entry.arrivalBonus()),
                actions(entry.arrivalActions()));
    }

    private static List<Gain> gains(List<String> raw) {
        return raw == null ? List.of() : raw.stream().map(Gain::fromCode).toList();
    }

    private static List<ActionType> actions(List<OceanTileDocument.ArrivalAction> raw) {
        return raw == null ? List.of() : raw.stream().map(action -> ActionType.fromCode(action.type())).toList();
    }
}
