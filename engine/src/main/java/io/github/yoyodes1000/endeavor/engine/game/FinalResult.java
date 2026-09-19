package io.github.yoyodes1000.endeavor.engine.game;

import java.util.List;

/**
 * Le résultat du décompte final : un score par joueur, les vainqueurs, et les
 * éléments du décompte que le moteur ne sait pas encore calculer.
 *
 * <p>Un score est <strong>incomplet</strong> quand un objectif de mission ou un
 * effectif Senior n'est pas encore câblé : il compte alors pour zéro, et la
 * raison est listée dans {@code uncounted}. On préfère ce résultat marqué à un
 * échec en fin de partie, mais l'appelant ne doit pas proclamer un vainqueur
 * définitif tant que {@link #isComplete()} est faux.
 *
 * @param scores    le détail par joueur, dans l'ordre des joueurs
 * @param winners   les indices des joueurs au score total maximal (plusieurs en cas d'égalité)
 * @param uncounted les éléments non comptés, vide si le décompte est complet
 */
public record FinalResult(List<PlayerScore> scores, List<Integer> winners, List<String> uncounted) {

    public FinalResult {
        scores = List.copyOf(scores);
        winners = List.copyOf(winners);
        uncounted = List.copyOf(uncounted);
    }

    public boolean isComplete() {
        return uncounted.isEmpty();
    }

    /**
     * @param attributesAndJournals les points des attributs et des revues
     * @param seniorEndGame         les points des décomptes Senior de rang 5
     * @param missionGoals          les points des objectifs de mission
     */
    public record PlayerScore(int attributesAndJournals, int seniorEndGame, int missionGoals) {

        public int total() {
            return attributesAndJournals + seniorEndGame + missionGoals;
        }
    }
}
