package io.github.yoyodes1000.endeavor.engine.player;

import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;

/**
 * Une tuile spécialiste possédée par un joueur : la tuile, sa face active, et le
 * nombre de disques posés dessus (lors d'une activation en Phase 2).
 *
 * <p>Immuable : la tuile référencée l'est déjà, et les évolutions (promotion,
 * disques posés ou repris) produiront une nouvelle valeur plutôt qu'une mutation.
 * Ces changements sont rares et hors de la boucle chaude de recherche, donc
 * l'immuabilité ne coûte rien ici — au contraire, elle simplifie la copie du
 * joueur, qui peut partager la référence.
 *
 * @param placedDiscs disques posés sur la tuile, jamais négatif
 */
public record HeldSpecialist(Specialist specialist, SpecialistFace face, int placedDiscs) {

    public HeldSpecialist {
        if (specialist == null) {
            throw new IllegalArgumentException("Une tuile détenue référence un spécialiste");
        }
        if (face == null) {
            throw new IllegalArgumentException("Une tuile détenue a une face active");
        }
        if (placedDiscs < 0) {
            throw new IllegalArgumentException("Nombre de disques posés négatif : " + placedDiscs);
        }
    }

    /** Une tuile fraîchement recrutée : face Junior, aucun disque posé. */
    public static HeldSpecialist recruited(Specialist specialist) {
        return new HeldSpecialist(specialist, SpecialistFace.JUNIOR, 0);
    }

    /** La face active de la tuile. */
    public SpecialistSide activeSide() {
        return specialist.side(face);
    }
}
