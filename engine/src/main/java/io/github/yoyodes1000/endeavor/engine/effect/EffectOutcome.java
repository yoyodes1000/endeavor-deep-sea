package io.github.yoyodes1000.endeavor.engine.effect;

/**
 * Ce qu'une résolution de gains a produit et qui reste à mettre en jeu : les
 * pions impact, les promotions et les choix d'attribut en attente, et les
 * submersibles gagnés.
 *
 * <p>La mise en jeu des impacts, promotions et choix d'attribut — poser un pion
 * sur le plateau de mission, choisir quel Junior promouvoir, choisir quelle
 * piste avancer — n'est pas faite ici : elle demande une décision et peut
 * relancer une cascade, donc elle revient à l'appelant, qui rappellera le
 * résolveur sur les récompenses obtenues.
 *
 * @param anyAttributeEarned    choix d'attribut libre en attente (toujours une
 *                              décision : les 4 pistes sont toujours valides)
 * @param lowestAttributeEarned choix d'attribut en attente entre pistes à
 *                              égalité au plus bas niveau — ne compte
 *                              <strong>que</strong> les cas ambigus : sans
 *                              égalité, {@link GainResolver} avance la piste
 *                              la plus basse directement, sans décision
 */
public record EffectOutcome(int impactsEarned, int vesselsEarned, int promotionsEarned, int anyAttributeEarned,
                            int lowestAttributeEarned) {

    /** Aucun effet à mettre en jeu. */
    public static final EffectOutcome NONE = new EffectOutcome(0, 0, 0, 0, 0);

    public EffectOutcome {
        if (impactsEarned < 0 || vesselsEarned < 0 || promotionsEarned < 0 || anyAttributeEarned < 0
                || lowestAttributeEarned < 0) {
            throw new IllegalArgumentException("Un décompte d'effets ne peut être négatif");
        }
    }

    /** La somme de deux résultats (pour cumuler une cascade). */
    public EffectOutcome plus(EffectOutcome other) {
        return new EffectOutcome(
                impactsEarned + other.impactsEarned,
                vesselsEarned + other.vesselsEarned,
                promotionsEarned + other.promotionsEarned,
                anyAttributeEarned + other.anyAttributeEarned,
                lowestAttributeEarned + other.lowestAttributeEarned);
    }
}
