package io.github.yoyodes1000.endeavor.engine.board;

/**
 * La position d'un cube sur une piste d'attribut (décision 5 de l'architecture).
 *
 * <p>Deux notions s'y superposent, et les confondre serait l'erreur de décalage
 * la plus probable du projet :
 * <ul>
 *   <li>le {@code step} (0 à 12) est la <em>position</em> stockée, ce qui avance
 *       d'un cran ; il part de 0 comme la piste imprimée sur le plateau ;
 *   <li>le {@code level} (1 à 5) est le <em>palier</em> atteint, <strong>déduit</strong>
 *       du step ; il part de 1 car il vaut exactement ce qu'il procure (au niveau
 *       3 on recrute au rang 3, on prend 3 disques…).
 * </ul>
 *
 * <p>La piste est bornée à 12 : au-delà, le cube revient à la case 10. Les
 * <em>conséquences</em> d'un franchissement (l'impact de la case 10, le vessel
 * de l'ingéniosité aux cases 2 et 7) sont des effets de jeu résolus ailleurs —
 * cette classe ne connaît que la position et le palier.
 *
 * @param step la case occupée, de 0 à 12
 */
public record AttributeTrack(int step) {

    private static final int MIN_STEP = 0;
    private static final int MAX_STEP = 12;

    /** Au-delà de la case 12, le cube revient ici (attributeTrackShape.overflowReturnCell). */
    private static final int OVERFLOW_RETURN_STEP = 10;

    /** Palier atteint pour chaque case 0..12 : les paliers 1 à 5 pèsent 2 + 2 + 3 + 3 + 3 cases. */
    private static final int[] LEVEL_BY_STEP = {1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5};

    /** Points marqués par palier ; l'indice est le level (1 à 5), l'indice 0 est inutilisé. */
    private static final int[] POINTS_BY_LEVEL = {0, 0, 1, 4, 7, 10};

    public AttributeTrack {
        if (step < MIN_STEP || step > MAX_STEP) {
            throw new IllegalArgumentException(
                    "Case de piste hors de " + MIN_STEP + ".." + MAX_STEP + " : " + step);
        }
    }

    /** La piste au départ de la partie : case 0, niveau 1. */
    public static AttributeTrack start() {
        return new AttributeTrack(MIN_STEP);
    }

    /** Le palier atteint (1 à 5), déduit de la case. */
    public int level() {
        return LEVEL_BY_STEP[step];
    }

    /** Les points rapportés par le palier atteint. */
    public int points() {
        return POINTS_BY_LEVEL[level()];
    }

    /**
     * Avance le cube de {@code steps} cases. Au-delà de la case 12, le cube
     * revient à la case 10 ; les effets du franchissement sont résolus ailleurs.
     *
     * @param steps le nombre de cases, positif ou nul
     * @return la piste après déplacement
     */
    public AttributeTrack advancedBy(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException(
                    "Une piste d'attribut n'avance pas à reculons : " + steps);
        }
        int raw = step + steps;
        int bounded = raw > MAX_STEP ? OVERFLOW_RETURN_STEP : raw;
        return new AttributeTrack(bounded);
    }
}
