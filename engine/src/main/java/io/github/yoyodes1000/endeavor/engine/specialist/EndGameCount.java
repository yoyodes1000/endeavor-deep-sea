package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Le registre des « effectifs » de décompte de fin de partie des neuf Senior de
 * rang 5 (décision 6 de l'architecture).
 *
 * <p>La frontière retenue : <em>l'arithmétique en données, le prédicat en
 * code</em>. Un {@link EndGameScoring} porte les nombres (points, diviseur) ;
 * chaque valeur ci-dessous nomme le calcul d'effectif correspondant. Les huit
 * calculs n'ayant presque rien en commun, les mettre en données exigerait
 * d'inventer un langage de requête sur l'état — plus de code que les neuf
 * fonctions qu'il remplacerait.
 *
 * <p>Ce registre sert d'abord au chargeur : un {@code count} absent d'ici fait
 * échouer le démarrage, au lieu de rapporter zéro point sans rien dire. Le
 * prédicat effectif de chacun sera branché quand le décompte final sera écrit.
 */
public enum EndGameCount {

    CONNECTIONS_SHARED_WITH_OPPONENTS("connections-shared-with-opponents"),
    CONSERVATION_DISCS_IN_BEST_ZONE("conservation-discs-in-best-zone"),
    COMPLETE_FIELD_SYMBOL_SETS("complete-field-symbol-sets"),
    DEPTHS_WITH_PUBLICATION("depths-with-publication"),
    ZONES_WITH_CONSERVATION("zones-with-conservation"),
    OTHER_SENIOR_SPECIALISTS("other-senior-specialists"),
    ZONES_WITH_DISC_OR_VESSEL("zones-with-disc-or-vessel"),
    EMPTY_DIVE_SITES("empty-dive-sites"),
    UNCONDITIONAL("unconditional");

    private final String code;

    EndGameCount(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Traduit un libellé des données en valeur du registre.
     *
     * @throws IllegalArgumentException si le libellé est inconnu — c'est la
     *     vérification voulue par la décision 6 : une faute de frappe échoue au
     *     chargement.
     */
    public static EndGameCount fromCode(String code) {
        for (EndGameCount count : values()) {
            if (count.code.equals(code)) {
                return count;
            }
        }
        throw new IllegalArgumentException("Décompte de fin de partie inconnu : " + code);
    }
}
