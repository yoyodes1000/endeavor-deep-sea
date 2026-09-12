package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Les actions qu'une tuile spécialiste peut offrir une fois activée : voyage,
 * sonar, plongée, conservation, publication, et la promotion (retourner un
 * spécialiste sur sa face Senior).
 *
 * <p>Comme {@link Gain}, le {@link #code()} est le libellé des données ; la
 * correspondance est centralisée ici.
 */
public enum ActionType {

    TRAVEL("travel"),
    SONAR("sonar"),
    DIVE("dive"),
    CONSERVE("conserve"),
    PUBLISH("publish"),
    PROMOTE("promote");

    private final String code;

    ActionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Traduit un libellé des données en valeur d'énumération.
     *
     * @throws IllegalArgumentException si le libellé est inconnu (échec au
     *     chargement, jamais d'action muette).
     */
    public static ActionType fromCode(String code) {
        for (ActionType action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Type d'action inconnu : " + code);
    }
}
