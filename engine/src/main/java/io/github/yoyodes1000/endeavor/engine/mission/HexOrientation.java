package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * L'orientation du pavage d'un plateau Impact. Elle détermine le calcul
 * d'adjacence (décision 7) : voisins est/ouest et décalage par lignes en
 * <em>pointe en haut</em>, nord/sud et décalage par colonnes en <em>côté plat</em>.
 * {@code rect-rows} désigne les pistes rectangulaires de la mission 10.
 */
public enum HexOrientation {

    POINTY_TOP("pointy-top"),
    FLAT_TOP("flat-top"),
    RECT_ROWS("rect-rows");

    private final String code;

    HexOrientation(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static HexOrientation fromCode(String code) {
        for (HexOrientation orientation : values()) {
            if (orientation.code.equals(code)) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("Orientation d'hexagones inconnue : " + code);
    }
}
