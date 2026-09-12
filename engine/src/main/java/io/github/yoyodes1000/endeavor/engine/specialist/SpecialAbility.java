package io.github.yoyodes1000.endeavor.engine.specialist;

/**
 * Une capacité spéciale décrite en texte libre (à ce jour, le seul cas est la
 * face Junior du Robotics Engineer : « copier l'action d'un autre spécialiste »).
 *
 * <p>On conserve le texte tel quel : sa mécanique n'est pas régulière, donc elle
 * ne se prête pas à une description en données. Le comportement, quand il faudra
 * l'implémenter, sera du code dédié — pas un effet générique.
 *
 * @param text le libellé exact de la tuile, non vide
 */
public record SpecialAbility(String text) {

    public SpecialAbility {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Le texte d'une capacité spéciale ne peut être vide");
        }
    }
}
