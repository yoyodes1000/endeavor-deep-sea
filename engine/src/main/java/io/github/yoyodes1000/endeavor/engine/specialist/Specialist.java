package io.github.yoyodes1000.endeavor.engine.specialist;

import java.util.OptionalInt;

/**
 * Une tuile spécialiste : un identifiant, un rang, et ses deux faces (Junior et
 * Senior). Le chef d'équipe est le cas particulier — il n'a pas de rang.
 *
 * <p>Les invariants du rang et du décompte de fin de partie sont vérifiés ici,
 * au plus près de la donnée : mieux vaut échouer au chargement qu'accepter une
 * tuile incohérente.
 *
 * @param rank       le rang (1 à 5), vide pour le chef d'équipe
 * @param teamLeader vrai pour l'unique chef d'équipe
 */
public record Specialist(
        String id,
        OptionalInt rank,
        boolean teamLeader,
        SpecialistSide junior,
        SpecialistSide senior) {

    private static final int MIN_RANK = 1;
    private static final int MAX_RANK = 5;

    /** Seuls les Senior de ce rang portent un décompte de fin de partie. */
    private static final int END_GAME_RANK = 5;

    public Specialist {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un spécialiste doit avoir un identifiant");
        }
        rank = rank == null ? OptionalInt.empty() : rank;
        if (junior == null || senior == null) {
            throw new IllegalArgumentException("Le spécialiste " + id + " doit avoir ses deux faces");
        }

        if (teamLeader) {
            if (rank.isPresent()) {
                throw new IllegalArgumentException("Le chef d'équipe (" + id + ") n'a pas de rang");
            }
        } else {
            if (rank.isEmpty()) {
                throw new IllegalArgumentException("Le spécialiste " + id + " doit avoir un rang");
            }
            int value = rank.getAsInt();
            if (value < MIN_RANK || value > MAX_RANK) {
                throw new IllegalArgumentException(
                        "Rang hors de " + MIN_RANK + ".." + MAX_RANK + " pour " + id + " : " + value);
            }
        }

        boolean endGameRank = rank.orElse(-1) == END_GAME_RANK;
        boolean seniorScores = senior.endGameScoring().isPresent();
        if (endGameRank && !seniorScores) {
            throw new IllegalArgumentException(
                    "Le Senior de rang 5 " + id + " doit porter un décompte de fin de partie");
        }
        if (!endGameRank && seniorScores) {
            throw new IllegalArgumentException(
                    "Seuls les Senior de rang 5 portent un décompte de fin de partie : " + id);
        }
        if (junior.endGameScoring().isPresent()) {
            throw new IllegalArgumentException(
                    "La face Junior de " + id + " ne peut porter un décompte de fin de partie");
        }
    }
}
