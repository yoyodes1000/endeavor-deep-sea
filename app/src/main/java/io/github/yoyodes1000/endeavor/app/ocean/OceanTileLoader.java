package io.github.yoyodes1000.endeavor.app.ocean;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.ocean.ConservationSite;
import io.github.yoyodes1000.endeavor.engine.ocean.DiveSite;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarSpot;
import io.github.yoyodes1000.endeavor.engine.ocean.SonarTrack;
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
                actions(entry.arrivalActions()),
                sonarTracks(entry.sonarTracks()),
                diveSites(entry.diveSites()),
                conservationSites(entry.conservationSites()));
    }

    private static List<Gain> gains(List<String> raw) {
        return raw == null ? List.of() : raw.stream().map(Gain::fromCode).toList();
    }

    private static List<ActionType> actions(List<OceanTileDocument.ArrivalAction> raw) {
        return raw == null ? List.of() : raw.stream().map(action -> ActionType.fromCode(action.type())).toList();
    }

    private static List<SonarTrack> sonarTracks(List<OceanTileDocument.Track> raw) {
        return raw == null ? List.of() : raw.stream().map(OceanTileLoader::toTrack).toList();
    }

    private static SonarTrack toTrack(OceanTileDocument.Track track) {
        List<OceanTileDocument.Spot> spots = track.spots();
        if (spots == null) {
            throw new IllegalArgumentException("Une piste Sonar doit décrire ses cases");
        }
        return new SonarTrack(spots.stream().map(OceanTileLoader::toSpot).toList());
    }

    private static SonarSpot toSpot(OceanTileDocument.Spot spot) {
        return switch (spot.type() == null ? "" : spot.type()) {
            case "reward" -> new SonarSpot.Reward(gains(spot.gains()));
            case "discover" -> new SonarSpot.Discover(spot.levels());
            default -> throw new IllegalArgumentException("Type de case Sonar inconnu : " + spot.type());
        };
    }

    private static List<DiveSite> diveSites(List<OceanTileDocument.DiveSite> raw) {
        return raw == null ? List.of() : raw.stream().map(OceanTileLoader::toDiveSite).toList();
    }

    private static DiveSite toDiveSite(OceanTileDocument.DiveSite site) {
        if (site.tokens() == null) {
            throw new IllegalArgumentException("Nombre de jetons manquant pour le site de plongée " + site.id());
        }
        return new DiveSite(site.id(), site.tokens());
    }

    private static List<ConservationSite> conservationSites(List<OceanTileDocument.ConservationSite> raw) {
        return raw == null ? List.of() : raw.stream().map(OceanTileLoader::toConservationSite).toList();
    }

    private static ConservationSite toConservationSite(OceanTileDocument.ConservationSite site) {
        if (site.cost() == null) {
            throw new IllegalArgumentException("Coût manquant pour le site de conservation " + site.id());
        }
        return new ConservationSite(site.id(), site.cost(), gains(site.gains()));
    }
}
