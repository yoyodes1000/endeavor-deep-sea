package io.github.yoyodes1000.endeavor.engine.specialist;

import java.util.List;
import java.util.Optional;

/**
 * Une des deux faces d'une tuile spécialiste (Junior ou Senior). Elle porte le
 * nom du personnage, ses gains immédiats à l'arrivée, la suite d'emplacements
 * d'action qu'elle offre une fois activée, et, selon les cas, une capacité
 * spéciale ou un décompte de fin de partie.
 *
 * @param immediateGains gains résolus au recrutement (peut être vide)
 * @param actions        emplacements d'action successifs (peut être vide)
 * @param specialAbility capacité spéciale éventuelle
 * @param endGameScoring décompte de fin de partie éventuel (Senior de rang 5)
 */
public record SpecialistSide(
        String name,
        List<Gain> immediateGains,
        List<ActionSlot> actions,
        Optional<SpecialAbility> specialAbility,
        Optional<EndGameScoring> endGameScoring) {

    public SpecialistSide {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom d'une face de spécialiste ne peut être vide");
        }
        immediateGains = List.copyOf(immediateGains);
        actions = List.copyOf(actions);
        specialAbility = specialAbility == null ? Optional.empty() : specialAbility;
        endGameScoring = endGameScoring == null ? Optional.empty() : endGameScoring;
    }
}
