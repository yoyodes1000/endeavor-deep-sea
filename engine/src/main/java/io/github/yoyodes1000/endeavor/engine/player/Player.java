package io.github.yoyodes1000.endeavor.engine.player;

import io.github.yoyodes1000.endeavor.engine.board.Attributes;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'état complet du plateau d'un joueur : ses pistes d'attribut, ses disques
 * d'action (en réserve et en zone de transit), et les tuiles spécialistes qu'il
 * détient.
 *
 * <p>Objet d'état mutable + {@link #copy()} (décision 3). La liste des tuiles est
 * exposée en lecture seule ; on la fait évoluer par des opérations dédiées (à
 * venir avec les étapes de la Phase 1).
 */
public final class Player {

    private final Attributes attributes;
    private int reserveDiscs;
    private int transitDiscs;
    private final List<HeldSpecialist> specialists;

    private Player(Attributes attributes, int reserveDiscs, int transitDiscs,
                   List<HeldSpecialist> specialists) {
        this.attributes = attributes;
        this.reserveDiscs = reserveDiscs;
        this.transitDiscs = transitDiscs;
        this.specialists = specialists;
    }

    /**
     * Un joueur en début de partie : pistes au départ, tous ses disques en
     * réserve, la zone de transit vide, et le chef d'équipe pour seule tuile.
     *
     * @param reserveDiscs le nombre de disques d'action de départ (mise en place)
     */
    public static Player start(Specialist teamLeader, int reserveDiscs) {
        if (teamLeader == null || !teamLeader.teamLeader()) {
            throw new IllegalArgumentException("Un joueur démarre avec un chef d'équipe");
        }
        if (reserveDiscs < 0) {
            throw new IllegalArgumentException("Nombre de disques en réserve négatif : " + reserveDiscs);
        }
        List<HeldSpecialist> specialists = new ArrayList<>();
        specialists.add(HeldSpecialist.recruited(teamLeader));
        return new Player(Attributes.atStart(), reserveDiscs, 0, specialists);
    }

    public Attributes attributes() {
        return attributes;
    }

    public int reserveDiscs() {
        return reserveDiscs;
    }

    public int transitDiscs() {
        return transitDiscs;
    }

    public List<HeldSpecialist> specialists() {
        return Collections.unmodifiableList(specialists);
    }

    /**
     * Déplace {@code count} disques de la réserve vers la zone de transit — le
     * mouvement de l'étape Effort (1b). Mutation en place.
     *
     * @throws IllegalArgumentException si la réserve est insuffisante
     */
    public void moveReserveToTransit(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Nombre de disques négatif : " + count);
        }
        if (count > reserveDiscs) {
            throw new IllegalArgumentException(
                    "Réserve insuffisante : " + count + " demandés, " + reserveDiscs + " disponibles");
        }
        reserveDiscs -= count;
        transitDiscs += count;
    }

    /** Ajoute une tuile fraîchement recrutée au plateau du joueur (étape 1a). */
    public void recruit(HeldSpecialist held) {
        if (held == null) {
            throw new IllegalArgumentException("Une tuile recrutée ne peut être nulle");
        }
        specialists.add(held);
    }

    /** Copie indépendante, appelée une fois par simulation pour l'isoler (déc. 3). */
    public Player copy() {
        return new Player(attributes.copy(), reserveDiscs, transitDiscs, new ArrayList<>(specialists));
    }
}
