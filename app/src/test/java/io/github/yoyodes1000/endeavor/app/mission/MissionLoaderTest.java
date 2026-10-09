package io.github.yoyodes1000.endeavor.app.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.mission.GoalOption;
import io.github.yoyodes1000.endeavor.engine.mission.SeaStarSide;
import io.github.yoyodes1000.endeavor.engine.mission.GoalUnit;
import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;
import io.github.yoyodes1000.endeavor.engine.mission.HexOrientation;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactBoard;
import io.github.yoyodes1000.endeavor.engine.mission.ImpactHex;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.mission.MissionGoal;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanSetup;
import io.github.yoyodes1000.endeavor.engine.ocean.StartingTile;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import io.github.yoyodes1000.endeavor.engine.ocean.HiddenTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.ShuffledRow;
import io.github.yoyodes1000.endeavor.app.ocean.OceanTileLoader;
import org.junit.jupiter.api.Test;

class MissionLoaderTest {

    private static final Path CATALOG = Path.of("..", "data", "missions.json");

    private final MissionLoader loader = new MissionLoader();

    private MissionCatalog realCatalog() throws IOException {
        try (Reader reader = Files.newBufferedReader(CATALOG, StandardCharsets.UTF_8)) {
            return loader.load(reader);
        }
    }

    @Test
    void chargeLesDixMissions() throws Exception {
        assertTrue(Files.exists(CATALOG), "Fichier introuvable : " + CATALOG.toAbsolutePath());
        MissionCatalog catalog = realCatalog();

        assertEquals(10, catalog.missions().size());
        Mission mission1 = catalog.byNumber(1).orElseThrow();
        assertEquals(HexOrientation.POINTY_TOP, mission1.impactBoard().orientation());
        assertEquals(32, mission1.impactBoard().hexes().size());
    }

    @Test
    void toutLePlateauDeMission1EstAtteignableDepuisLesDeparts() throws Exception {
        ImpactBoard board = realCatalog().byNumber(1).orElseThrow().impactBoard();

        assertEquals(4, board.startHexes().size());
        assertEquals(board.hexes().size(), board.reachableFromStarts().size(),
                "contrôle de relevé (déc. 7) : tout hexagone atteignable depuis un départ");
    }

    @Test
    void uneCaseDeDepartPorteSaRecompense() throws Exception {
        ImpactHex depart = realCatalog().byNumber(1).orElseThrow().impactBoard().hexAt(0, 0).orElseThrow();
        assertTrue(depart.start());
        assertEquals(List.of(Gain.REPUTATION), depart.gains());
    }

    @Test
    void refuseUnGainInconnu() {
        String json = "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\",\"impactBoard\":"
                + "{\"orientation\":\"pointy-top\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0,\"gains\":[\"gold\"]}]}}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    private static String boardWith(String hexFields) {
        return "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\",\"impactBoard\":"
                + "{\"orientation\":\"pointy-top\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0," + hexFields + "}]}}]}";
    }

    private ImpactHex loadedHex(String hexFields) {
        return loader.load(new StringReader(boardWith(hexFields))).byNumber(1).orElseThrow()
                .impactBoard().hexAt(0, 0).orElseThrow();
    }

    private List<ImpactHex> realHexes() throws Exception {
        return realCatalog().missions().stream().flatMap(mission -> mission.impactBoard().hexes().stream()).toList();
    }

    @Test
    void chargeLesSymbolesDeDomaineDeCouleur() throws Exception {
        List<ImpactHex> colored = realHexes().stream().filter(hex -> hex.fieldSymbol().isPresent()).toList();
        assertFalse(colored.isEmpty());
        assertTrue(colored.stream().allMatch(hex -> !hex.wild() && hex.fieldSymbolCount() == 1));
        assertTrue(colored.stream().anyMatch(hex -> hex.fieldSymbol().get() == FieldSymbol.YELLOW));
    }

    @Test
    void chargeLesJokersEtLesSymbolesDoubles() throws Exception {
        List<ImpactHex> wilds = realHexes().stream().filter(ImpactHex::wild).toList();
        assertFalse(wilds.isEmpty());
        assertTrue(wilds.stream().allMatch(hex -> hex.fieldSymbol().isEmpty()));
        assertTrue(wilds.stream().anyMatch(hex -> hex.fieldSymbolCount() == 2), "un joker double (relevé)");
    }

    @Test
    void unHexagoneSansSymboleNEnPortePas() throws Exception {
        ImpactHex plain = realCatalog().byNumber(1).orElseThrow().impactBoard().hexAt(0, 0).orElseThrow();
        assertEquals(Optional.empty(), plain.fieldSymbol());
        assertFalse(plain.wild());
        assertEquals(0, plain.fieldSymbolCount());
    }

    @Test
    void refuseUneCouleurDeSymboleInconnue() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> loadedHex("\"fieldSymbol\":\"purple\""));
        assertTrue(error.getMessage().contains("purple"));
    }

    @Test
    void refuseUneOrientationInconnue() {
        String json = "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\",\"impactBoard\":"
                + "{\"orientation\":\"triangle\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0}]}}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void chargeLaMiseEnPlaceDeLOceanDeMission1() throws Exception {
        OceanSetup setup = realCatalog().byNumber(1).orElseThrow().oceanSetup();

        assertEquals(5, setup.columns());
        assertEquals(4, setup.startingTiles().size());

        StartingTile.Named seaStar = setup.startingTiles().stream()
                .filter(tile -> tile instanceof StartingTile.Named named && named.tileId().equals("the-sea-star"))
                .map(StartingTile.Named.class::cast)
                .findFirst().orElseThrow();
        assertEquals(1, seaStar.depth());
        assertEquals(2, seaStar.col(), "colonne C → indice 2");

        StartingTile.Random drawn = setup.startingTiles().stream()
                .filter(StartingTile.Random.class::isInstance)
                .map(StartingTile.Random.class::cast)
                .findFirst().orElseThrow();
        assertEquals(1, drawn.level());
        assertEquals(3, drawn.col(), "colonne D → indice 3");
    }

    @Test
    void mission1ATroisObjectifsStandardAuxUnitesAttendues() throws Exception {
        List<MissionGoal> goals = realCatalog().byNumber(1).orElseThrow().goals();

        assertEquals(3, goals.size());
        List<MissionGoal.Standard> standards = goals.stream()
                .map(MissionGoal.Standard.class::cast)
                .toList();
        assertEquals(List.of(GoalUnit.SONAR), standards.get(0).units());
        assertEquals(List.of(GoalUnit.PUBLISH), standards.get(1).units());
        assertEquals(List.of(GoalUnit.CONSERVE), standards.get(2).units());
        assertEquals(5, standards.get(0).majorityBonus().orElseThrow().first());
    }

    @Test
    void unObjectifABonusParCouleurEstStandard() throws Exception {
        // Mission 3, objectif 3 : bonus { blue, brown, green, yellow } au lieu de { first, second }.
        MissionGoal.Standard goal = (MissionGoal.Standard) realCatalog().byNumber(3).orElseThrow().goals().get(2);

        assertEquals(List.of(GoalUnit.FIELD_SYMBOL), goal.units());
        assertEquals(4, goal.colorBonuses().size());
        assertTrue(goal.colorBonuses().stream().allMatch(bonus -> bonus.points() == 2));
        assertTrue(goal.majorityBonus().isEmpty());
    }

    @Test
    void unObjectifDeZonesDecouvertesABonusDeLeaderEstStandard() throws Exception {
        // Mission 6, objectif 2 : zones découvertes, +2 au leader des profondeurs 3, 4, 5 et des colonnes B, D.
        MissionGoal.Standard goal = (MissionGoal.Standard) realCatalog().byNumber(6).orElseThrow().goals().get(1);

        assertTrue(goal.discoveredByYou());
        assertEquals(5, goal.leaderBonuses().size());
        assertEquals(List.of(3), goal.leaderBonuses().get(0).depths());
        assertEquals(List.of(1), goal.leaderBonuses().get(3).columns(), "colonne B -> indice 1");
        assertEquals(List.of(3), goal.leaderBonuses().get(4).columns(), "colonne D -> indice 3");
    }

    @Test
    void unObjectifADisqueDesigneResteNonSupporte() throws Exception {
        // Mission 6, objectif 3 : prédicat count + bonus de leader par unité.
        assertTrue(realCatalog().byNumber(6).orElseThrow().goals().get(2) instanceof MissionGoal.Unsupported);
    }

    @Test
    void lesObjectifsAOptionsDeMission8SontDesChoix() throws Exception {
        List<MissionGoal> goals = realCatalog().byNumber(8).orElseThrow().goals();

        assertTrue(goals.stream().allMatch(goal -> goal instanceof MissionGoal.Choice));
        MissionGoal.Choice near = (MissionGoal.Choice) goals.get(0);
        assertEquals(List.of("near", "far"), near.options().stream().map(GoalOption::id).toList());
        assertEquals(Optional.of(SeaStarSide.LEFT), near.options().get(0).goal().fromSeaStar());
        assertEquals(Optional.of(SeaStarSide.RIGHT), near.options().get(1).goal().fromSeaStar());
        MissionGoal.Choice symbols = (MissionGoal.Choice) goals.get(2);
        assertEquals(List.of("narrow", "broad"), symbols.options().stream().map(GoalOption::id).toList());
        assertEquals(List.of(GoalUnit.FIELD_SYMBOL_SET), symbols.options().get(1).goal().units());
        assertEquals(3, symbols.options().get(1).goal().pointsPer());
    }

    @Test
    void lesHexagonesObjectifDeMission8PortentLeurNumero() throws Exception {
        ImpactBoard board = realCatalog().byNumber(8).orElseThrow().impactBoard();

        assertEquals(java.util.OptionalInt.of(1), board.hexAt(9, 0).orElseThrow().goal());
        assertEquals(java.util.OptionalInt.of(2), board.hexAt(8, 8).orElseThrow().goal());
        assertEquals(java.util.OptionalInt.of(3), board.hexAt(1, 0).orElseThrow().goal());
    }

    @Test
    void refuseUneColonneInvalideDansLaMiseEnPlace() {
        String json = "{\"missions\":[{\"id\":\"m\",\"number\":1,\"name\":\"M\","
                + "\"impactBoard\":{\"orientation\":\"pointy-top\",\"hexes\":[{\"row\":0,\"col\":0,\"points\":0}]},"
                + "\"setup\":{\"columns\":5,\"startingTiles\":[{\"depth\":1,\"col\":\"AA\",\"tile\":\"x\"}]}}]}";
        assertThrows(IllegalArgumentException.class, () -> loader.load(new StringReader(json)));
    }

    @Test
    void lesMissionsAZoneDeLancementParTuileLaDeclarent() throws Exception {
        MissionCatalog catalog = realCatalog();

        for (int number : new int[]{2, 4, 8, 9, 10}) {
            Mission mission = catalog.byNumber(number).orElseThrow();
            assertEquals(Optional.of("the-sea-star"), mission.baseTile(), "mission " + number);
            assertEquals(1, mission.startingVessels(), "mission " + number);
        }
        assertEquals(Optional.of("remote-anchorage"), catalog.byNumber(7).orElseThrow().baseTile());
        assertTrue(catalog.byNumber(1).orElseThrow().baseOfOperations().isPresent(), "M1 garde sa case");
    }

    @Test
    void lesMissionsADeuxZonesDeLancementChoisiesNeLaDeclarentPasEncore() throws Exception {
        for (int number : new int[]{3, 5, 6}) {
            Mission mission = realCatalog().byNumber(number).orElseThrow();
            assertTrue(mission.baseOfOperations().isEmpty() && mission.baseTile().isEmpty(), "mission " + number);
        }
    }

    @Test
    void lesProfondeursMaximalesEtLesColonnesDesMissionsReduitesSontChargees() throws Exception {
        OceanSetup mission5 = realCatalog().byNumber(5).orElseThrow().oceanSetup();
        assertEquals(7, mission5.columns());
        assertEquals(3, mission5.maxDepth());
        assertEquals(6, realCatalog().byNumber(9).orElseThrow().oceanSetup().columns());
        assertEquals(4, realCatalog().byNumber(9).orElseThrow().oceanSetup().maxDepth());
        assertEquals(5, realCatalog().byNumber(1).orElseThrow().oceanSetup().maxDepth(), "profondeur pleine par défaut");
    }

    @Test
    void lesLignesMelangeesEtLesTuilesCacheesDeMission8SontChargees() throws Exception {
        OceanSetup setup = realCatalog().byNumber(8).orElseThrow().oceanSetup();

        assertEquals(1, setup.shuffledRows().size());
        assertEquals(List.of(0, 1, 2, 3, 4), setup.shuffledRows().get(0).columns());
        assertEquals(new ShuffledRow.Named("the-sea-star"), setup.shuffledRows().get(0).entries().get(0));
        assertEquals(new ShuffledRow.Random(1), setup.shuffledRows().get(0).entries().get(1));
        assertEquals(List.of(new HiddenTile("the-looking-glass", 2)), setup.hiddenTiles());
    }

    @Test
    void toutesLesTuilesNommeesDesMissionsExistentAuCatalogueDesTuiles() throws Exception {
        try (Reader source = Files.newBufferedReader(Path.of("..", "data", "ocean-tiles.json"), StandardCharsets.UTF_8)) {
            OceanTileCatalog tiles = new OceanTileLoader().load(source);
            for (Mission mission : realCatalog().missions()) {
                OceanSetup setup = mission.oceanSetup();
                setup.startingTiles().stream().filter(tile -> tile instanceof StartingTile.Named)
                        .forEach(tile -> assertTrue(tiles.byId(((StartingTile.Named) tile).tileId()).isPresent(),
                                "mission " + mission.number() + " : " + tile));
                setup.hiddenTiles().forEach(hidden -> assertTrue(tiles.byId(hidden.tileId()).isPresent(),
                        "mission " + mission.number() + " : " + hidden));
            }
        }
    }
}
