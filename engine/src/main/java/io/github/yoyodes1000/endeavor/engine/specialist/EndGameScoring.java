package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Le décompte de fin de partie d'un Senior de rang 5 (décision 6).
 *
 * <p>Les neuf décomptes partagent la même arithmétique — {@code points × partie
 * entière(effectif ÷ per)} — mais chacun compte un effectif différent. On garde
 * donc ici les nombres (données) et le nom du calcul ({@link EndGameCount}, un
 * prédicat codé), et {@link #text} pour l'affichage et pour vérifier que la
 * fonction codée dit bien la même chose que la tuile.
 *
 * @param text   le libellé exact de la tuile, non vide
 * @param points les points accordés par tranche
 * @param per    la taille d'une tranche (diviseur), au moins 1
 * @param count  le calcul d'effectif, garanti connu du registre
 */
public record EndGameScoring(String text, int points, int per, EndGameCount count) {

    public EndGameScoring {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Le texte d'un décompte de fin de partie ne peut être vide");
        }
        if (points < 0) {
            throw new IllegalArgumentException(
                    "Les points d'un décompte doivent être positifs ou nuls : " + points);
        }
        if (per < 1) {
            throw new IllegalArgumentException(
                    "Le diviseur d'un décompte doit valoir au moins 1 : " + per);
        }
        if (count == null) {
            throw new IllegalArgumentException("Un décompte doit nommer son calcul d'effectif");
        }
    }

    /**
     * Applique l'arithmétique commune aux neuf décomptes à un effectif déjà
     * calculé par le prédicat codé.
     *
     * @param effectif la grandeur mesurée sur l'état (nombre de zones, de sites…)
     * @return les points obtenus
     */
    public int score(int effectif) {
        return points * (effectif / per);
    }
}
