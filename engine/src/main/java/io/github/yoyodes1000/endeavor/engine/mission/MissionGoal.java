package io.github.yoyodes1000.endeavor.engine.mission;

import java.util.List;
import java.util.Optional;

/**
 * Un objectif de fin de mission (décision : l'arithmétique en données, le
 * prédicat en code, comme les décomptes Senior). La fiche de chaque mission en
 * porte trois.
 *
 * <p>Deux formes : {@link Standard} couvre le cas courant (compter des unités ou des
 * zones, filtrées par profondeur/colonne, un point par unité, un bonus de majorité,
 * des bonus de leader par profondeur ou colonne, des bonus par couleur de symbole) ;
 * {@link Unsupported} couvre tout le reste — objectifs à options, prédicats spéciaux
 * nommés (piste de nettoyage, premier à la profondeur 5…) et bonus de leader par
 * unité. Ces cartons portent une carte sœur ; en attendant, {@code text} reste
 * disponible pour l'affichage, mais aucun calcul de points n'existe.
 */
public sealed interface MissionGoal permits MissionGoal.Standard, MissionGoal.Choice, MissionGoal.Unsupported {

    int number();

    String text();

    /**
     * @param units           ce qu'on compte ; {@code zone} ne se combine jamais avec
     *                        une autre unité
     * @param depths          les profondeurs qui filtrent le compte, {@code []} = toutes
     * @param columns         les colonnes qui filtrent le compte (indices 0-based),
     *                        {@code []} = toutes
     * @param zoneContains    pour un objectif {@code zone} : la zone compte si elle
     *                        porte au moins une de ces unités pour le joueur ; vide sauf
     *                        pour {@code zone}
     * @param pointsPer       les points par unité comptée (ou par zone qualifiée)
     * @param majorityBonus   le bonus de majorité, s'il y en a un
     */
    record Standard(
            int number,
            List<GoalUnit> units,
            List<Integer> depths,
            List<Integer> columns,
            List<GoalUnit> zoneContains,
            int pointsPer,
            Optional<MajorityBonus> majorityBonus,
            String text,
            boolean discoveredByYou,
            List<LeaderBonus> leaderBonuses,
            List<ColorBonus> colorBonuses,
            Optional<SeaStarSide> fromSeaStar) implements MissionGoal {

        public Standard {
            if (number < 1) {
                throw new IllegalArgumentException("Numéro d'objectif invalide : " + number);
            }
            if (units == null || units.isEmpty()) {
                throw new IllegalArgumentException("L'objectif " + number + " doit compter au moins une unité");
            }
            units = List.copyOf(units);
            if (units.contains(GoalUnit.ZONE) && units.size() > 1) {
                throw new IllegalArgumentException(
                        "« zone » ne se combine pas avec d'autres unités : objectif " + number);
            }
            depths = List.copyOf(depths == null ? List.of() : depths);
            columns = List.copyOf(columns == null ? List.of() : columns);
            zoneContains = List.copyOf(zoneContains == null ? List.of() : zoneContains);
            if ((units.contains(GoalUnit.FIELD_SYMBOL) || units.contains(GoalUnit.FIELD_SYMBOL_SET))
                    && units.size() > 1) {
                throw new IllegalArgumentException(
                        "Une unité de symboles de domaine ne se combine pas avec d'autres unités : objectif " + number);
            }
            fromSeaStar = fromSeaStar == null ? Optional.empty() : fromSeaStar;
            if (!zoneContains.isEmpty() && !units.contains(GoalUnit.ZONE)) {
                throw new IllegalArgumentException(
                        "zoneContains suppose l'unité « zone » : objectif " + number);
            }
            if (pointsPer < 1) {
                throw new IllegalArgumentException("Points par unité invalides pour l'objectif " + number);
            }
            majorityBonus = majorityBonus == null ? Optional.empty() : majorityBonus;
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("L'objectif " + number + " doit avoir un texte");
            }
            if (discoveredByYou && !units.contains(GoalUnit.ZONE)) {
                throw new IllegalArgumentException(
                        "« découverte par vous » suppose l'unité « zone » : objectif " + number);
            }
            if (units.contains(GoalUnit.ZONE) && zoneContains.isEmpty() && !discoveredByYou) {
                throw new IllegalArgumentException(
                        "Une zone se qualifie par zoneContains ou par sa découverte : objectif " + number);
            }
            leaderBonuses = List.copyOf(leaderBonuses == null ? List.of() : leaderBonuses);
            colorBonuses = List.copyOf(colorBonuses == null ? List.of() : colorBonuses);
            if (!colorBonuses.isEmpty() && !units.contains(GoalUnit.FIELD_SYMBOL)) {
                throw new IllegalArgumentException(
                        "Un bonus par couleur suppose l'unité « fieldSymbol » : objectif " + number);
            }
        }

        /** Un objectif sans côté de la sea-star. */
        public Standard(int number, List<GoalUnit> units, List<Integer> depths, List<Integer> columns,
                        List<GoalUnit> zoneContains, int pointsPer, Optional<MajorityBonus> majorityBonus,
                        String text, boolean discoveredByYou, List<LeaderBonus> leaderBonuses,
                        List<ColorBonus> colorBonuses) {
            this(number, units, depths, columns, zoneContains, pointsPer, majorityBonus, text, discoveredByYou,
                    leaderBonuses, colorBonuses, Optional.empty());
        }

        /** Un objectif sans découverte ni bonus de leader ni bonus par couleur. */
        public Standard(int number, List<GoalUnit> units, List<Integer> depths, List<Integer> columns,
                        List<GoalUnit> zoneContains, int pointsPer, Optional<MajorityBonus> majorityBonus,
                        String text) {
            this(number, units, depths, columns, zoneContains, pointsPer, majorityBonus, text,
                    false, List.of(), List.of(), Optional.empty());
        }
    }

    /**
     * Un objectif à options : le joueur qui pose un pion sur son hexagone objectif
     * choisit l'option, qui vaut ensuite pour tous les joueurs. Tant que l'hexagone
     * est vide, l'objectif ne rapporte rien à personne.
     *
     * @param options les options offertes, au moins deux, d'identifiants distincts
     */
    record Choice(int number, List<GoalOption> options) implements MissionGoal {

        public Choice {
            if (number < 1) {
                throw new IllegalArgumentException("Numéro d'objectif invalide : " + number);
            }
            if (options == null || options.size() < 2) {
                throw new IllegalArgumentException("L'objectif " + number + " doit offrir au moins deux options");
            }
            options = List.copyOf(options);
            if (options.stream().map(GoalOption::id).distinct().count() != options.size()) {
                throw new IllegalArgumentException("Identifiants d'option en double : objectif " + number);
            }
        }

        @Override
        public String text() {
            return options.stream().map(option -> option.goal().text()).reduce((a, b) -> a + " / " + b).orElse("");
        }

        public Optional<GoalOption> option(String optionId) {
            return options.stream().filter(option -> option.id().equals(optionId)).findFirst();
        }
    }

    /**
     * @param text le texte de la fiche, ou vide quand l'objectif n'en porte pas
     *             au niveau racine (un objectif à options porte le sien par
     *             option, pas ici)
     */
    record Unsupported(int number, String text) implements MissionGoal {

        public Unsupported {
            if (number < 1) {
                throw new IllegalArgumentException("Numéro d'objectif invalide : " + number);
            }
            text = text == null ? "" : text;
        }
    }
}
