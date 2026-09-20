package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * Le côté de la sea-star où un objectif compte : la colonne de la sea-star et
 * toutes celles qui sont à sa gauche ({@link #LEFT}) ou à sa droite ({@link #RIGHT}).
 */
public enum SeaStarSide {

    LEFT("left"),
    RIGHT("right");

    private final String code;

    SeaStarSide(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * @throws IllegalArgumentException si le libellé est inconnu
     */
    public static SeaStarSide fromCode(String code) {
        for (SeaStarSide side : values()) {
            if (side.code.equals(code)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Côté de la sea-star inconnu : " + code);
    }

    /** Vrai si la colonne compte pour ce côté, la sea-star étant en {@code seaStarColumn}. */
    public boolean accepts(int column, int seaStarColumn) {
        return this == LEFT ? column <= seaStarColumn : column >= seaStarColumn;
    }
}
