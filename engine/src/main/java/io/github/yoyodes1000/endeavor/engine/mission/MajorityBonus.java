package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * Le bonus de majorité d'un objectif standard : au meneur, au second. La
 * répartition en cas d'égalité (parts égales des tranches concernées,
 * arrondi à l'inférieur) est l'affaire du calculateur, pas de cette donnée.
 *
 * @param first  le bonus du joueur en tête
 * @param second le bonus du deuxième
 */
public record MajorityBonus(int first, int second) {

    public MajorityBonus {
        if (first < 0 || second < 0) {
            throw new IllegalArgumentException("Un bonus de majorité ne peut être négatif : " + first + "/" + second);
        }
    }
}
