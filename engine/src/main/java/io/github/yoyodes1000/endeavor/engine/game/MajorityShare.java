package io.github.yoyodes1000.endeavor.engine.game;

/**
 * La part d'un joueur dans un bonus, d'après les effectifs de tous les joueurs.
 * Un joueur à effectif nul ne touche jamais rien, même à égalité de tous à zéro.
 *
 * <p>Règle des ex æquo (règle du jeu) : <em>bonus à deux tranches</em> — les joueurs
 * à égalité en tête touchent chacun la moyenne (1er + 2e) ÷ 2, arrondie à
 * l'inférieur, quel que soit leur nombre ; aucun 2e n'existe alors. Les joueurs à
 * égalité pour la 2e place touchent chacun la tranche du 2e entière.
 * <em>Bonus de leader</em> (une seule tranche) — chaque leader ex æquo touche le
 * bonus entier.
 */
final class MajorityShare {

    private MajorityShare() {
    }

    static int twoTiers(int[] effectifs, int player, int first, int second) {
        if (effectifs[player] == 0) {
            return 0;
        }
        int top = max(effectifs, Integer.MAX_VALUE);
        if (effectifs[player] == top) {
            return countEqual(effectifs, top) == 1 ? first : (first + second) / 2;
        }
        if (countEqual(effectifs, top) > 1) {
            return 0;
        }
        return effectifs[player] == max(effectifs, top) ? second : 0;
    }

    static int leader(int[] effectifs, int player, int points) {
        return effectifs[player] > 0 && effectifs[player] == max(effectifs, Integer.MAX_VALUE) ? points : 0;
    }

    /** Le plus grand effectif strictement inférieur à {@code below}. */
    private static int max(int[] effectifs, int below) {
        int best = 0;
        for (int effectif : effectifs) {
            if (effectif < below) {
                best = Math.max(best, effectif);
            }
        }
        return best;
    }

    private static int countEqual(int[] effectifs, int value) {
        int count = 0;
        for (int effectif : effectifs) {
            if (effectif == value) {
                count++;
            }
        }
        return count;
    }
}
