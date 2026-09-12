package io.github.yoyodes1000.endeavor.engine.board;

/**
 * Les quatre pistes d'attribut du plateau joueur, dans l'ordre d'affichage :
 * réputation, inspiration, coordination, ingéniosité.
 *
 * <p>Chaque attribut est aussi un gain possible, mais l'inverse est faux : la
 * recherche et l'impact sont des gains sans piste d'attribut. D'où une
 * énumération dédiée, distincte du vocabulaire des gains.
 */
public enum Attribute {

    REPUTATION("reputation"),
    INSPIRATION("inspiration"),
    COORDINATION("coordination"),
    INGENUITY("ingenuity");

    private final String code;

    Attribute(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Traduit un libellé des données en attribut.
     *
     * @throws IllegalArgumentException si le libellé est inconnu.
     */
    public static Attribute fromCode(String code) {
        for (Attribute attribute : values()) {
            if (attribute.code.equals(code)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Attribut inconnu : " + code);
    }
}
