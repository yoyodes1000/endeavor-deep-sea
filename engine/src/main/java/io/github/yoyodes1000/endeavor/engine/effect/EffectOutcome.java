package io.github.yoyodes1000.endeavor.engine.effect;

/**
 * Ce qu'une résolution de gains a produit et qui reste à mettre en jeu : les
 * pions impact et les submersibles gagnés.
 *
 * <p>Leur mise en jeu — poser un pion sur le plateau de mission, faire arriver un
 * submersible sur une tuile — n'est pas faite ici : elle demande une décision et
 * peut relancer une cascade, donc elle revient à l'appelant, qui rappellera le
 * résolveur sur les récompenses obtenues.
 */
public record EffectOutcome(int impactsEarned, int vesselsEarned) {

    /** Aucun effet à mettre en jeu. */
    public static final EffectOutcome NONE = new EffectOutcome(0, 0);

    public EffectOutcome {
        if (impactsEarned < 0 || vesselsEarned < 0) {
            throw new IllegalArgumentException("Un décompte d'effets ne peut être négatif");
        }
    }

    /** La somme de deux résultats (pour cumuler une cascade). */
    public EffectOutcome plus(EffectOutcome other) {
        return new EffectOutcome(
                impactsEarned + other.impactsEarned,
                vesselsEarned + other.vesselsEarned);
    }
}
