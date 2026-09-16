package io.github.yoyodes1000.endeavor.engine.journal;

/**
 * Les symboles de domaine imprimés sur les revues scientifiques et les sites
 * de publication : une revue ne peut être publiée que sur un site qui en porte
 * un exemplaire.
 *
 * <p>Comme {@link io.github.yoyodes1000.endeavor.engine.specialist.Gain}, le
 * {@link #code()} est le libellé exact employé dans les données du matériel.
 */
public enum FieldSymbol {

    BLUE("blue"),
    YELLOW("yellow"),
    BROWN("brown"),
    GREEN("green");

    private final String code;

    FieldSymbol(String code) {
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
    public static FieldSymbol fromCode(String code) {
        for (FieldSymbol symbol : values()) {
            if (symbol.code.equals(code)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("Symbole de domaine inconnu : " + code);
    }
}
