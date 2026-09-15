package io.github.yoyodes1000.endeavor.app.mission;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;
import io.github.yoyodes1000.endeavor.engine.ocean.StartingTile;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * Charge le catalogue des missions depuis le JSON du matériel et le traduit vers
 * le modèle du moteur (identité + plateau Impact + mise en place de l'océan).
 *
 * <p>Même partage que les autres chargeurs : Jackson lit la forme, le moteur
 * valide le vocabulaire et la sémantique. Agnostique de l'I/O ({@link Reader}).
 * La colonne d'une tuile de départ, lettre dans les données, est convertie en
 * indice numérique ici, à la frontière. Les champs de la fiche non encore
 * modélisés (objectifs, règles spéciales, base d'opérations, marqueurs
 * d'hexagone) sont tolérés.
 */
public final class MissionLoader {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MissionCatalog load(Reader source) {
        MissionsDocument document = read(source);
        if (document == null || document.missions() == null) {
            throw new IllegalArgumentException("JSON des missions vide ou sans tableau « missions »");
        }
        List<Mission> missions = document.missions().stream()
                .map(MissionLoader::toMission)
                .toList();
        return new MissionCatalog(missions);
    }

    private MissionsDocument read(Reader source) {
        try {
            return mapper.readValue(source, MissionsDocument.class);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON des missions illisible", e);
        }
    }

    private static Mission toMission(MissionsDocument.Entry entry) {
        if (entry.number() == null) {
            throw new IllegalArgumentException("Mission sans numéro : " + entry.id());
        }
        MissionsDocument.SetupDto setup = entry.setup();
        return new Mission(entry.id(), entry.number(), entry.name(), toBoard(entry.id(), entry.impactBoard()),
                toOceanSetup(entry.id(), setup), toBaseOfOperations(setup), startingVessels(setup));
    }

    private static Optional<Cell> toBaseOfOperations(MissionsDocument.SetupDto setup) {
        MissionsDocument.CellDto base = setup.baseOfOperations();
        if (base == null) {
            return Optional.empty();
        }
        if (base.depth() == null || base.col() == null) {
            throw new IllegalArgumentException("Base d'opérations incomplète (depth et col requis)");
        }
        return Optional.of(new Cell(base.depth(), columnIndex(base.col())));
    }

    private static int startingVessels(MissionsDocument.SetupDto setup) {
        return setup.startingVessels() == null ? 0 : setup.startingVessels();
    }

    private static ImpactBoard toBoard(String missionId, MissionsDocument.ImpactBoardDto board) {
        if (board == null) {
            throw new IllegalArgumentException("La mission " + missionId + " n'a pas de plateau Impact");
        }
        HexOrientation orientation = HexOrientation.fromCode(board.orientation());
        List<ImpactHex> hexes = board.hexes().stream().map(MissionLoader::toHex).toList();
        return new ImpactBoard(orientation, hexes);
    }

    private static ImpactHex toHex(MissionsDocument.HexDto hex) {
        if (hex.row() == null || hex.col() == null || hex.points() == null) {
            throw new IllegalArgumentException("Hexagone incomplet (row, col et points requis)");
        }
        List<Gain> gains = hex.gains() == null ? List.of() : hex.gains().stream().map(Gain::fromCode).toList();
        boolean unlimited = "unlimited".equals(hex.capacity());
        return new ImpactHex(
                hex.row(), hex.col(), hex.points(), gains,
                Boolean.TRUE.equals(hex.start()), Boolean.TRUE.equals(hex.offGrid()), unlimited);
    }

    private static OceanSetup toOceanSetup(String missionId, MissionsDocument.SetupDto setup) {
        if (setup == null || setup.columns() == null) {
            throw new IllegalArgumentException("La mission " + missionId + " n'a pas de mise en place d'océan (colonnes)");
        }
        List<StartingTile> startingTiles = setup.startingTiles() == null ? List.of()
                : setup.startingTiles().stream().map(MissionLoader::toStartingTile).toList();
        return new OceanSetup(setup.columns(), startingTiles);
    }

    private static StartingTile toStartingTile(MissionsDocument.StartingTileDto tile) {
        if (tile.depth() == null || tile.col() == null) {
            throw new IllegalArgumentException("Tuile de mise en place incomplète (depth et col requis)");
        }
        int col = columnIndex(tile.col());
        if (tile.tile() != null) {
            return new StartingTile.Named(tile.depth(), col, tile.tile());
        }
        if (tile.randomLevel() != null) {
            return new StartingTile.Random(tile.depth(), col, tile.randomLevel());
        }
        throw new IllegalArgumentException(
                "Tuile de mise en place sans « tile » ni « randomLevel » en colonne " + tile.col());
    }

    /** Convertit une lettre de colonne (A, B, …) en indice 0-based pour le moteur. */
    private static int columnIndex(String col) {
        if (col.length() != 1 || col.charAt(0) < 'A' || col.charAt(0) > 'Z') {
            throw new IllegalArgumentException("Colonne invalide (une lettre A–Z attendue) : " + col);
        }
        return col.charAt(0) - 'A';
    }
}
