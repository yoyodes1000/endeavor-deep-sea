package io.github.yoyodes1000.endeavor.engine.play;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.dive.DiveSiteSetup;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.GainResolver;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.journal.JournalCatalog;
import io.github.yoyodes1000.endeavor.engine.mission.Mission;
import io.github.yoyodes1000.endeavor.engine.mission.MissionBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.HiddenTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanBoard;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTileCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;

/**
 * Mise en place de l'océan au démarrage d'une partie : chaque joueur reçoit ses
 * submersibles et en pose un sur la <strong>base d'opérations</strong>, dont il
 * encaisse aussitôt le bonus d'arrivée — <strong>avant</strong> le premier
 * recrutement (Phase 1). En mission 1, la base est {@code the-sea-star} et son
 * bonus d'arrivée est un simple disque.
 *
 * <p>À appeler après {@code GameState.newGame} et avant {@link Game#begin}, comme
 * les autres gestes de mise en place bâtis par l'appelant (plateaux, océan).
 */
public final class GameSetup {

    private GameSetup() {
    }

    /** Le matériel immuable d'une partie, commun à toutes les missions. */
    public record Materials(SpecialistRoster roster, OceanTileCatalog oceanTiles, DiveTokenCatalog diveTokens,
                            JournalCatalog journals) {
    }

    /**
     * Bâtit une partie prête à démarrer ({@link Game#begin}) pour une mission : l'océan de sa mise en
     * place (lignes mélangées comprises), la pioche de découverte (tuiles cachées ajoutées, tuiles
     * au-delà de la profondeur maximale retirées), les submersibles de départ sur la zone de lancement
     * et les jetons des sites de plongée déjà en jeu.
     *
     * @throws IllegalArgumentException si la fiche ne désigne pas de zone de lancement, ou si une tuile
     *     cachée est inconnue du matériel ou n'est pas de son niveau
     */
    public static GameState newMissionGame(Mission mission, int playerCount, Materials materials,
                                           RandomSource random, int startingDiscs) {
        OceanBoard ocean = OceanBoard.fromSetup(mission.oceanSetup(), materials.oceanTiles(), random);
        Cell launch = mission.launchCell(ocean).orElseThrow(() -> new IllegalArgumentException(
                "La mission " + mission.number() + " ne désigne pas de zone de lancement"));
        GameState state = GameState.newGame(playerCount, materials.roster(), random, startingDiscs,
                MissionBoard.forMission(mission), ocean, materials.oceanTiles(), materials.diveTokens(),
                materials.journals());
        shapeDiscoveryPile(state, mission);
        deployStartingVessels(state, launch, mission.startingVessels());
        stackInitialDiveSites(state);
        return state;
    }

    private static void shapeDiscoveryPile(GameState state, Mission mission) {
        for (HiddenTile hidden : mission.oceanSetup().hiddenTiles()) {
            OceanTile tile = state.oceanTileCatalog().byId(hidden.tileId()).orElseThrow(
                    () -> new IllegalArgumentException("Tuile cachée inconnue au catalogue : " + hidden.tileId()));
            if (tile.depth() != hidden.level()) {
                throw new IllegalArgumentException("La tuile cachée " + hidden.tileId() + " est de niveau "
                        + tile.depth() + ", pas " + hidden.level());
            }
            state.discoveryPile().addTile(hidden.tileId(), hidden.level());
        }
        state.discoveryPile().removeDeeperThan(mission.oceanSetup().maxDepth());
    }

    /**
     * Dote chaque joueur de {@code vesselsPerPlayer} submersibles, en pose un sur la
     * base d'opérations, et résout son bonus d'arrivée (gains directs ; submersibles
     * éventuellement gagnés versés au stock).
     *
     * @throws IllegalArgumentException si la base ne porte pas de tuile connue, ou si
     *     le nombre de submersibles de départ est inférieur à un
     * @throws IllegalStateException si le bonus d'arrivée de la base produit un impact,
     *     qui n'est pas résolu à la mise en place (aucun scénario connu ne le fait)
     */
    public static void deployStartingVessels(GameState state, Cell base, int vesselsPerPlayer) {
        if (base == null) {
            throw new IllegalArgumentException("La base d'opérations est requise");
        }
        if (vesselsPerPlayer < 1) {
            throw new IllegalArgumentException(
                    "Il faut au moins un submersible de départ : " + vesselsPerPlayer);
        }
        String tileId = state.oceanBoard().tileAt(base).orElseThrow(
                () -> new IllegalArgumentException("Base d'opérations sans tuile : " + base));
        OceanTile tile = state.oceanTileCatalog().byId(tileId).orElseThrow(
                () -> new IllegalArgumentException("Tuile de base inconnue au catalogue : " + tileId));
        for (int player = 0; player < state.playerCount(); player++) {
            state.player(player).gainVessels(vesselsPerPlayer);
            state.player(player).takeVesselFromStock();
            state.oceanBoard().addVessels(base, player, 1);
            EffectOutcome arrival = GainResolver.resolve(state.player(player), tile.arrivalBonus());
            state.player(player).gainVessels(arrival.vesselsEarned());
            if (arrival.impactsEarned() > 0) {
                throw new IllegalStateException(
                        "Le bonus d'arrivée de la base produit un impact, non résolu à la mise en place");
            }
        }
    }

    /**
     * Empile les jetons de plongée des sites déjà en jeu à la mise en place — un
     * geste par site, dans la pioche partagée. À appeler une fois par partie, après
     * {@code GameState.newGame}.
     */
    public static void stackInitialDiveSites(GameState state) {
        for (Cell cell : state.oceanBoard().occupiedCells()) {
            String tileId = state.oceanBoard().tileAt(cell).orElseThrow();
            OceanTile tile = state.oceanTileCatalog().byId(tileId).orElseThrow(
                    () -> new IllegalStateException("Tuile inconnue au catalogue : " + tileId));
            DiveSiteSetup.stack(state.oceanBoard(), state.diveTokenPile(), state.random(), cell, tile);
        }
    }
}
