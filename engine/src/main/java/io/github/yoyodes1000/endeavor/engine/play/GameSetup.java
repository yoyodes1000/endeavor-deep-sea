package io.github.yoyodes1000.endeavor.engine.play;

import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.effect.GainResolver;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import io.github.yoyodes1000.endeavor.engine.ocean.OceanTile;

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
}
