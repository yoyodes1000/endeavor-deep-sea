package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Les six ressources qu'un spécialiste peut procurer immédiatement à son
 * recrutement : les quatre attributs (inspiration, coordination, ingéniosité,
 * réputation) plus la recherche et l'impact.
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
    IMPACT("impact");

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
