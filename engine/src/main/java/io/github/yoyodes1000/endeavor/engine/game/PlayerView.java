package io.github.yoyodes1000.endeavor.engine.game;

import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ce qu'un joueur donné peut légitimement observer de la partie (décision 2).
 *
 * <p>L'IA ne reçoit jamais que cette vue, jamais le {@link GameState} complet :
 * c'est ce qui l'empêchera mécaniquement de lire l'information cachée. En Phase 1
 * presque tout est public (le casier, les plateaux adverses), donc la vue expose
 * l'essentiel ; le filtrage de l'information cachée (piles face cachée) prendra
 * son sens avec la Phase 2.
 */
public final class PlayerView {

    private final GameState state;
    private final int viewer;

    PlayerView(GameState state, int viewer) {
        if (viewer < 0 || viewer >= state.playerCount()) {
            throw new IllegalArgumentException(
                    "Indice de joueur hors bornes : " + viewer + " pour " + state.playerCount() + " joueur(s)");
        }
        this.state = state;
        this.viewer = viewer;
    }

    public int viewerIndex() {
        return viewer;
    }

    /** Le plateau du joueur qui observe. */
    public Player self() {
        return state.player(viewer);
    }

    /** Les plateaux adverses (information publique), dans l'ordre des places. */
    public List<Player> opponents() {
        List<Player> opponents = new ArrayList<>();
        for (int i = 0; i < state.playerCount(); i++) {
            if (i != viewer) {
                opponents.add(state.player(i));
            }
        }
        return Collections.unmodifiableList(opponents);
    }

    /** Le casier partagé des tuiles recrutables. */
    public List<Specialist> casier() {
        return state.casier();
    }

    public int round() {
        return state.round();
    }

    public int firstPlayerIndex() {
        return state.firstPlayerIndex();
    }
}
