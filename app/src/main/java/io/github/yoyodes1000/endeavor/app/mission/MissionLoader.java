package io.github.yoyodes1000.endeavor.app.mission;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Charge le catalogue des missions depuis le JSON du matériel et le traduit vers
 * le modèle du moteur (identité + plateau Impact).
 *
 * <p>Même partage que les autres chargeurs : Jackson lit la forme, le moteur
 * valide le vocabulaire et la sémantique. Agnostique de l'I/O ({@link Reader}).
 * Les champs de la fiche non encore modélisés (mise en place, objectifs, règles,
 * marqueurs d'hexagone) sont tolérés.
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
        return new Mission(entry.id(), entry.number(), entry.name(), toBoard(entry.id(), entry.impactBoard()));
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
}
