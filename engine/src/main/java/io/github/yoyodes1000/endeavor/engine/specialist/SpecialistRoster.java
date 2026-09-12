package io.github.yoyodes1000.endeavor.engine.specialist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le casier partagé de toutes les tuiles spécialistes de la partie.
 *
 * <p>Le constructeur vérifie les invariants d'ensemble : identifiants uniques et
 * exactement un chef d'équipe. Comme pour le reste du modèle, une incohérence
 * fait échouer le chargement plutôt que de se glisser dans une partie.
 */
public record SpecialistRoster(List<Specialist> specialists) {

    public SpecialistRoster {
        if (specialists == null || specialists.isEmpty()) {
            throw new IllegalArgumentException("Le casier des spécialistes est vide");
        }
        specialists = List.copyOf(specialists);

        Map<String, Specialist> byId = new LinkedHashMap<>();
        int teamLeaders = 0;
        for (Specialist specialist : specialists) {
            if (byId.put(specialist.id(), specialist) != null) {
                throw new IllegalArgumentException("Identifiant de spécialiste en double : " + specialist.id());
            }
            if (specialist.teamLeader()) {
                teamLeaders++;
            }
        }
        if (teamLeaders != 1) {
            throw new IllegalArgumentException(
                    "Il faut exactement un chef d'équipe, trouvé : " + teamLeaders);
        }
    }

    /** Le spécialiste portant cet identifiant, s'il existe. */
    public Optional<Specialist> byId(String id) {
        return specialists.stream().filter(specialist -> specialist.id().equals(id)).findFirst();
    }

    /** L'unique chef d'équipe (garanti présent par le constructeur). */
    public Specialist teamLeader() {
        return specialists.stream()
                .filter(Specialist::teamLeader)
                .findFirst()
                .orElseThrow();
    }

    /** Les tuiles d'un rang donné, dans l'ordre du casier. */
    public List<Specialist> ofRank(int rank) {
        return specialists.stream()
                .filter(specialist -> specialist.rank().orElse(-1) == rank)
                .toList();
    }
}
