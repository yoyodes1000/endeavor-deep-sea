package io.github.yoyodes1000.endeavor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.yoyodes1000.endeavor.app.dive.DiveTokenLoader;
import io.github.yoyodes1000.endeavor.app.journal.JournalLoader;
import io.github.yoyodes1000.endeavor.app.mission.MissionLoader;
import io.github.yoyodes1000.endeavor.app.ocean.OceanTileLoader;
import io.github.yoyodes1000.endeavor.app.specialist.SpecialistLoader;
import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.game.FinalResult;
import io.github.yoyodes1000.endeavor.engine.game.GamePhase;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.play.Game;
import io.github.yoyodes1000.endeavor.engine.play.GameSetup;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Assemble de <strong>vraies parties</strong> à partir des fichiers du matériel, mission par
 * mission, et les joue jusqu'au décompte. Jalon de vérité : la mise en place réelle et les règles
 * tiennent debout de bout en bout, sans exception.
 *
 * <p>Les missions jouables sont celles dont la fiche désigne une zone de lancement. Les autres
 * (M3, M5 : la zone se choisit ; M6 : à préciser) n'y figurent pas encore.
 */
class RealGameMissionsTest {

    private static final Path DATA = Path.of("..", "data");

    private static final List<Integer> PLAYABLE_MISSIONS = List.of(1, 2, 4, 7, 8, 9, 10);

    /** Garde-fou : une partie réelle tient en bien moins de coups que cela. */
    private static final int MAX_MOVES = 20_000;

    private static Reader reader(String file) throws IOException {
        return Files.newBufferedReader(DATA.resolve(file), StandardCharsets.UTF_8);
    }

    private static MissionCatalog missions() throws IOException {
        try (Reader source = reader("missions.json")) {
            return new MissionLoader().load(source);
        }
    }

    /** Assemble la mise en place réelle d'une mission (sans démarrer la partie). */
    private static GameState newMissionGame(int missionNumber, int players, long seed) throws IOException {
        Mission mission = missions().byNumber(missionNumber).orElseThrow();
        GameSetup.Materials materials;
        try (Reader ocean = reader("ocean-tiles.json"); Reader specialists = reader("specialists.json");
             Reader diveTokens = reader("dive-tokens.json"); Reader journals = reader("journals.json")) {
            materials = new GameSetup.Materials(new SpecialistLoader().load(specialists),
                    new OceanTileLoader().load(ocean), new DiveTokenLoader().load(diveTokens),
                    new JournalLoader().load(journals));
        }
        return GameSetup.newMissionGame(mission, players, materials, RandomSource.fromSeed(seed), 6);
    }

    @Test
    void assembleEtDemarreUneVraiePartieDeMission1() throws Exception {
        int players = 3;
        GameState state = newMissionGame(1, players, 2026);
        Game.begin(state);
        OceanBoard ocean = state.oceanBoard();

        // la base d'opérations de M1 : the-sea-star en C1 (colonne C → indice 2)
        Cell base = new Cell(1, 2);
        assertEquals("the-sea-star", ocean.tileAt(base).orElseThrow());
        for (int player = 0; player < players; player++) {
            assertEquals(1, ocean.vesselCount(base, player), "un submersible de chaque joueur sur la base");
            assertEquals(0, state.player(player).vesselStock(), "départ à 1, déployé 1");
        }

        // les deux sites de plongée de départ (volcanic-island B1, the-sea-star C1) sont empilés
        assertEquals(2, state.oceanBoard().diveTokenCount(new Cell(1, 1), "d1"), "volcanic-island : 2 jetons");
        assertEquals(4, state.oceanBoard().diveTokenCount(base, "d1"), "the-sea-star : 4 jetons");

        // la partie est démarrée et attend une décision (le premier recrutement)
        assertEquals(GamePhase.PREPARATION, state.phase());
        assertFalse(Game.legalActions(state).isEmpty(), "des coups sont proposés au premier joueur");
    }

    @Test
    void chaqueMissionJouableLanceSesSubmersiblesSurSaZoneDeLancement() throws Exception {
        for (int number : PLAYABLE_MISSIONS) {
            Mission mission = missions().byNumber(number).orElseThrow();
            GameState state = newMissionGame(number, 3, 7);
            Cell launch = mission.launchCell(state.oceanBoard()).orElseThrow();

            for (int player = 0; player < 3; player++) {
                assertEquals(1, state.oceanBoard().vesselCount(launch, player),
                        "mission " + number + " : un submersible de chaque joueur en " + launch);
            }
        }
    }

    @Test
    void laSeaStarMelangeeDeMission8SertDeZoneDeLancementOuQuElleTombe() throws Exception {
        java.util.Set<Integer> seaStarColumns = new java.util.HashSet<>();
        for (long seed = 1; seed <= 30; seed++) {
            GameState state = newMissionGame(8, 2, seed);
            Cell launch = missions().byNumber(8).orElseThrow().launchCell(state.oceanBoard()).orElseThrow();

            assertEquals("the-sea-star", state.oceanBoard().tileAt(launch).orElseThrow());
            assertEquals(1, state.oceanBoard().vesselCount(launch, 0));
            seaStarColumns.add(launch.col());
        }
        assertTrue(seaStarColumns.size() > 1, "la sea-star ne tombe pas toujours dans la même colonne");
    }

    @Test
    void joueLesSixManchesDesVraiesPartiesJusquAuDecompte() throws Exception {
        for (int number : PLAYABLE_MISSIONS) {
            int gamesWithPoints = 0;
            for (int players = 1; players <= 4; players++) {
                for (long seed = 1; seed <= 5; seed++) {
                    FinalResult result = playToTheEnd(number, players, seed);
                    if (result.scores().stream().anyMatch(score -> score.total() > 0)) {
                        gamesWithPoints++;
                    }
                }
            }
            // le jeu aléatoire passe souvent : toutes les parties ne marquent pas, mais certaines doivent le faire
            assertTrue(gamesWithPoints > 0,
                    "mission " + number + " : aucune des 20 parties n'a marqué de point, décompte suspect");
        }
    }

    private static FinalResult playToTheEnd(int missionNumber, int players, long seed) throws IOException {
        String context = "mission " + missionNumber + ", " + players + " joueurs, graine " + seed;
        GameState state = newMissionGame(missionNumber, players, seed);
        Game.begin(state);
        RandomSource picker = RandomSource.fromSeed(seed * 31 + players);

        int moves = 0;
        while (state.phase() != GamePhase.FINISHED) {
            List<Action> legal = Game.legalActions(state);
            assertFalse(legal.isEmpty(), "un coup est légal tant que la partie continue (" + context + ")");
            Game.apply(state, legal.get(picker.nextInt(legal.size())));
            if (++moves > MAX_MOVES) {
                fail("la partie ne se termine pas (" + context + ")");
            }
        }

        assertEquals(6, state.round(), context);
        FinalResult result = Game.finalResult(state);
        assertEquals(players, result.scores().size(), context);
        assertFalse(result.winners().isEmpty(), context);
        if (missionNumber == 1) {
            assertTrue(result.isComplete(), "M1 : rien ne doit rester non compté (" + context + ") " + result.uncounted());
        }
        return result;
    }
}
