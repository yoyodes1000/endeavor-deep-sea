package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Les jetons de gain du jeu, employés aussi bien par les spécialistes que par
 * les tuiles Océan : les quatre attributs (inspiration, coordination,
 * ingéniosité, réputation), la recherche, l'impact, le disque d'action, l'avancée
 * d'un attribut au choix ({@code anyAttribute}) ou du plus bas
 * ({@code lowestAttribute}), et la promotion d'un spécialiste.
 *
 * <p>Le {@link #code()} est le libellé exact employé dans les données du
 * matériel. Cette correspondance vit dans le moteur pour qu'il n'existe qu'une
 * seule source de vérité du vocabulaire : le chargeur s'y réfère au lieu de
 * réinventer les chaînes.
 */
public enum Gain {

    INSPIRATION("inspiration"),
    COORDINATION("coordination"),
    INGENUITY("ingenuity"),
    REPUTATION("reputation"),
    RESEARCH("research"),
    IMPACT("impact"),
    DISC("disc"),
    ANY_ATTRIBUTE("anyAttribute"),
    LOWEST_ATTRIBUTE("lowestAttribute"),
    PROMOTE("promote");

    private final String code;

    Gain(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Traduit un libellé des données en valeur d'énumération.
     *
     * @throws IllegalArgumentException si le libellé est inconnu — une faute de
     *     frappe dans les données fait échouer le chargement plutôt que d'être
     *     ignorée en silence.
     */
    public static Gain fromCode(String code) {
        for (Gain gain : values()) {
            if (gain.code.equals(code)) {
                return gain;
            }
        }
        throw new IllegalArgumentException("Gain inconnu : " + code);
    }
}
