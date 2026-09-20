package io.github.yoyodes1000.endeavor.engine.mission;

/**
 * Ce qu'un objectif de fin de mission compte, dans le vocabulaire fermé du
 * matériel : {@code sonar}, {@code publish} et {@code conserve} comptent les
 * disques d'un type de site précis ; {@code disc} n'importe quel disque ;
 * {@code vessel} les submersibles ; {@code zone} des zones (toujours seule,
 * qualifiée par {@code zoneContains} sur {@link MissionGoal.Standard}) ;
 * {@code fieldSymbol} les symboles de domaine du plateau Impact (toujours seule) ;
 * {@code fieldSymbolSet} les jeux complets des quatre couleurs (toujours seule) ;
 * {@code impactMarker} reste hors de portée (cf. {@link MissionGoal.Unsupported}).
 */
public enum GoalUnit {

    SONAR("sonar"),
    PUBLISH("publish"),
    CONSERVE("conserve"),
    DISC("disc"),
    VESSEL("vessel"),
    ZONE("zone"),
    FIELD_SYMBOL("fieldSymbol"),
    FIELD_SYMBOL_SET("fieldSymbolSet"),
    IMPACT_MARKER("impactMarker");

    private final String code;

    GoalUnit(String code) {
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
    public static GoalUnit fromCode(String code) {
        for (GoalUnit unit : values()) {
            if (unit.code.equals(code)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unité d'objectif inconnue : " + code);
    }
}
