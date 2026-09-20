package io.github.yoyodes1000.endeavor.app.mission;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.ColorBonus;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.LeaderBonus;
import io.github.yoyodes1000.endeavor.engine.mission.MajorityBonus;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;
import io.github.yoyodes1000.endeavor.engine.ocean.StartingTile;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Charge le catalogue des missions depuis le JSON du matériel et le traduit vers
 * le modèle du moteur (identité + plateau Impact + mise en place de l'océan).
 *
 * <p>Même partage que les autres chargeurs : Jackson lit la forme, le moteur
 * valide le vocabulaire et la sémantique. Agnostique de l'I/O ({@link Reader}).
 * La colonne d'une tuile de départ, lettre dans les données, est convertie en
 * indice numérique ici, à la frontière — de même pour les colonnes d'un
 * objectif. Les règles spéciales et les marqueurs d'hexagone autres que les
 * symboles de domaine (flèches…) restent tolérés, non modélisés.
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
        List<MissionGoal> goals = entry.goals() == null ? List.of()
                : entry.goals().stream().map(MissionLoader::toGoal).toList();
        return new Mission(entry.id(), entry.number(), entry.name(), toBoard(entry.id(), entry.impactBoard()),
                toOceanSetup(entry.id(), setup), toBaseOfOperations(setup), startingVessels(setup), goals);
    }

    private static MissionGoal toGoal(MissionsDocument.GoalDto goal) {
        if (goal.number() == null) {
            throw new IllegalArgumentException("Objectif sans numéro");
        }
        String text = goal.text() == null ? "" : goal.text();
        if (!isStandardShape(goal)) {
            return new MissionGoal.Unsupported(goal.number(), text);
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("Objectif " + goal.number() + " sans texte");
        }
        List<GoalUnit> units = goal.units().stream().map(GoalUnit::fromCode).toList();
        List<Integer> depths = goal.depths() == null ? List.of() : goal.depths();
        List<Integer> columns = goal.columns() == null ? List.of() : toColumnIndexes(goal.columns());
        List<GoalUnit> zoneContains = goal.zoneContains() == null ? List.of()
                : goal.zoneContains().stream().map(GoalUnit::fromCode).toList();
        int pointsPer = goal.pointsPer() == null ? 1 : goal.pointsPer();
        Optional<MajorityBonus> majorityBonus = isTierBonus(goal.majorityBonus())
                ? Optional.of(new MajorityBonus(goal.majorityBonus().get("first"), goal.majorityBonus().get("second")))
                : Optional.empty();
        List<ColorBonus> colorBonuses = isTierBonus(goal.majorityBonus()) || goal.majorityBonus() == null
                ? List.of() : toColorBonuses(goal.majorityBonus());
        List<LeaderBonus> leaderBonuses = goal.leaderBonuses() == null ? List.of()
                : goal.leaderBonuses().stream().map(MissionLoader::toLeaderBonus).toList();
        return new MissionGoal.Standard(goal.number(), units, depths, columns, zoneContains, pointsPer,
                majorityBonus, text, Boolean.TRUE.equals(goal.zoneDiscoveredByYou()), leaderBonuses, colorBonuses);
    }

    /**
     * Vrai si l'objectif se ramène à une forme que le moteur calcule : filtre standard,
     * bonus de majorité {@code first}/{@code second}, bonus par couleur, bonus de leader
     * par profondeur ou colonne, découverte de zone. Tout le reste — {@code chooseOption},
     * prédicat {@code count}, {@code columnsFromSeaStar}, bonus de leader par unité — devient
     * un objectif {@link MissionGoal.Unsupported}, carte sœur.
     */
    private static boolean isStandardShape(MissionsDocument.GoalDto goal) {
        if (goal.count() != null || goal.columnsFromSeaStar() != null || Boolean.TRUE.equals(goal.chooseOption())) {
            return false;
        }
        if (goal.units() == null || goal.units().isEmpty()) {
            return false;
        }
        boolean leaderBonusesUnderstood = goal.leaderBonuses() == null
                || goal.leaderBonuses().stream().allMatch(MissionLoader::isPositionalLeaderBonus);
        return leaderBonusesUnderstood
                && (goal.majorityBonus() == null || isTierBonus(goal.majorityBonus())
                || isColorBonus(goal.majorityBonus(), goal.units()));
    }

    private static boolean isPositionalLeaderBonus(MissionsDocument.LeaderBonusDto bonus) {
        boolean targetsPosition = (bonus.depths() != null && !bonus.depths().isEmpty())
                || (bonus.columns() != null && !bonus.columns().isEmpty());
        return bonus.points() != null && bonus.units() == null && targetsPosition;
    }

    private static boolean isTierBonus(Map<String, Integer> bonus) {
        return bonus != null && bonus.keySet().equals(Set.of("first", "second"));
    }

    private static boolean isColorBonus(Map<String, Integer> bonus, List<String> units) {
        return units.contains(GoalUnit.FIELD_SYMBOL.code())
                && bonus.keySet().stream().allMatch(key -> Arrays.stream(FieldSymbol.values())
                .anyMatch(color -> color.code().equals(key)));
    }

    private static List<ColorBonus> toColorBonuses(Map<String, Integer> bonus) {
        return bonus.entrySet().stream()
                .map(entry -> new ColorBonus(FieldSymbol.fromCode(entry.getKey()), entry.getValue()))
                .toList();
    }

    private static LeaderBonus toLeaderBonus(MissionsDocument.LeaderBonusDto bonus) {
        List<Integer> columns = bonus.columns() == null ? List.of() : toColumnIndexes(bonus.columns());
        return new LeaderBonus(bonus.depths(), columns, bonus.points());
    }

    private static List<Integer> toColumnIndexes(List<String> letters) {
        return letters.stream().map(MissionLoader::columnIndex).toList();
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
        boolean wild = "wild".equals(hex.fieldSymbol());
        Optional<FieldSymbol> color = hex.fieldSymbol() == null || wild
                ? Optional.empty() : Optional.of(FieldSymbol.fromCode(hex.fieldSymbol()));
        int symbolCount = hex.fieldSymbol() == null ? 0
                : hex.fieldSymbolCount() == null ? 1 : hex.fieldSymbolCount();
        return new ImpactHex(
                hex.row(), hex.col(), hex.points(), gains,
                Boolean.TRUE.equals(hex.start()), Boolean.TRUE.equals(hex.offGrid()), unlimited,
                color, wild, symbolCount);
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
