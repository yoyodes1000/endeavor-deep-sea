package io.github.yoyodes1000.endeavor.engine.player;

import io.github.yoyodes1000.endeavor.engine.board.Attributes;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L'état complet du plateau d'un joueur : ses pistes d'attribut, ses disques
 * d'action (en réserve et en zone de transit), ses submersibles non déployés, et
 * les tuiles spécialistes qu'il détient.
 *
 * <p>Objet d'état mutable + {@link #copy()} (décision 3). La liste des tuiles est
 * exposée en lecture seule ; on la fait évoluer par des opérations dédiées (à
 * venir avec les étapes de la Phase 1).
 */
public final class Player {

    private static final int RESEARCH_CAP = 12;

    private final Attributes attributes;
    private int reserveDiscs;
    private int transitDiscs;
    private int research;
    private int vesselStock;
    private final List<HeldSpecialist> specialists;
    private final List<String> heldDiveTokens;
    private final List<String> journals;

    private Player(Attributes attributes, int reserveDiscs, int transitDiscs, int research,
                   int vesselStock, List<HeldSpecialist> specialists, List<String> heldDiveTokens,
                   List<String> journals) {
        this.attributes = attributes;
        this.reserveDiscs = reserveDiscs;
        this.transitDiscs = transitDiscs;
        this.research = research;
        this.vesselStock = vesselStock;
        this.specialists = specialists;
        this.heldDiveTokens = heldDiveTokens;
        this.journals = journals;
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
        return new Player(Attributes.atStart(), reserveDiscs, 0, 0, 0, specialists, new ArrayList<>(),
                new ArrayList<>());
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

    public int research() {
        return research;
    }

    /** Les submersibles non déployés du joueur (sa réserve, hors grille). */
    public int vesselStock() {
        return vesselStock;
    }

    /** Ajoute des submersibles à la réserve — le gain d'ingéniosité (cases 2 et 7). */
    public void gainVessels(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Gain de submersibles négatif : " + count);
        }
        vesselStock += count;
    }

    /**
     * Sort un submersible de la réserve pour le mettre en jeu (mise en place sur la
     * base d'opérations, déploiement). Mutation en place.
     *
     * @throws IllegalArgumentException si la réserve de submersibles est vide
     */
    public void takeVesselFromStock() {
        if (vesselStock == 0) {
            throw new IllegalArgumentException("Aucun submersible en réserve");
        }
        vesselStock--;
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

    /** Avance la piste de recherche de {@code count} crans, plafonnée à 12. */
    public void gainResearch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Gain de recherche négatif : " + count);
        }
        research = Math.min(RESEARCH_CAP, research + count);
    }

    /**
     * Dépense {@code count} points de recherche — le coût d'une Conservation.
     * Mutation en place.
     *
     * @throws IllegalArgumentException si la recherche disponible est insuffisante
     */
    public void spendResearch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Dépense de recherche négative : " + count);
        }
        if (count > research) {
            throw new IllegalArgumentException(
                    "Recherche insuffisante : " + count + " demandés, " + research + " disponibles");
        }
        research -= count;
    }

    /** Ajoute {@code count} disques d'action à la réserve. */
    public void gainDiscs(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Gain de disques négatif : " + count);
        }
        reserveDiscs += count;
    }

    /** Ajoute une tuile fraîchement recrutée au plateau du joueur (étape 1a). */
    public void recruit(HeldSpecialist held) {
        if (held == null) {
            throw new IllegalArgumentException("Une tuile recrutée ne peut être nulle");
        }
        specialists.add(held);
    }

    /** Vrai si au moins une tuile détenue porte un disque récupérable (étape 1c). */
    public boolean hasRecoverableDisc() {
        return specialists.stream().anyMatch(held -> held.placedDiscs() > 0);
    }

    /**
     * Active la tuile d'identifiant donné : pose un disque de la zone de transit sur
     * sa case d'activation libre — le geste central d'un tour de Phase 2. Mutation
     * en place.
     *
     * @throws IllegalArgumentException si le joueur ne détient pas cette tuile, si sa
     *     case d'activation est déjà occupée, ou si la zone de transit est vide
     */
    public void activate(String specialistId) {
        if (transitDiscs == 0) {
            throw new IllegalArgumentException("Aucun disque en transit pour activer : " + specialistId);
        }
        for (int i = 0; i < specialists.size(); i++) {
            HeldSpecialist held = specialists.get(i);
            if (held.specialist().id().equals(specialistId)) {
                if (held.placedDiscs() > 0) {
                    throw new IllegalArgumentException("Case d'activation déjà occupée : " + specialistId);
                }
                specialists.set(i, new HeldSpecialist(held.specialist(), held.face(), held.placedDiscs() + 1));
                transitDiscs--;
                return;
            }
        }
        throw new IllegalArgumentException("Tuile non détenue : " + specialistId);
    }

    /**
     * Dépense un disque de la zone de transit pour le poser sur un site du plateau
     * (le coût « +1 disque » d'un Sonar, plus tard d'une Conservation ou d'une
     * Publication). Le disque quitte le joueur — posé sur la grille, il n'est pas
     * repris en 1c et ne compte pas au décompte. Mutation en place.
     *
     * @throws IllegalArgumentException si la zone de transit est vide
     */
    public void spendTransitDisc() {
        if (transitDiscs == 0) {
            throw new IllegalArgumentException("Aucun disque en transit à dépenser");
        }
        transitDiscs--;
    }

    /**
     * Reprend un disque posé sur la tuile d'identifiant donné vers la zone de
     * transit — un pas de l'étape 1c (Récupération). Mutation en place.
     *
     * @throws IllegalArgumentException si le joueur ne détient pas cette tuile, ou
     *     si aucun disque n'y est posé
     */
    public void recoverDisc(String specialistId) {
        for (int i = 0; i < specialists.size(); i++) {
            HeldSpecialist held = specialists.get(i);
            if (held.specialist().id().equals(specialistId)) {
                if (held.placedDiscs() == 0) {
                    throw new IllegalArgumentException("Aucun disque à reprendre sur : " + specialistId);
                }
                specialists.set(i, new HeldSpecialist(held.specialist(), held.face(), held.placedDiscs() - 1));
                transitDiscs++;
                return;
            }
        }
        throw new IllegalArgumentException("Tuile non détenue : " + specialistId);
    }

    /**
     * Retourne la tuile Junior d'identifiant donné côté Senior — la Promotion.
     * Le disque posé sur la case Junior est perdu (pas rendu au joueur) ; la
     * nouvelle case d'activation Senior repart libre. Mutation en place.
     *
     * @return la tuile côté Senior, pour encaisser ses gains immédiats
     * @throws IllegalArgumentException si le joueur ne détient pas cette tuile côté
     *     Junior
     */
    public HeldSpecialist promote(String specialistId) {
        for (int i = 0; i < specialists.size(); i++) {
            HeldSpecialist held = specialists.get(i);
            if (held.specialist().id().equals(specialistId)) {
                if (held.face() != SpecialistFace.JUNIOR) {
                    throw new IllegalArgumentException("Déjà côté Senior : " + specialistId);
                }
                HeldSpecialist promoted = new HeldSpecialist(held.specialist(), SpecialistFace.SENIOR, 0);
                specialists.set(i, promoted);
                return promoted;
            }
        }
        throw new IllegalArgumentException("Tuile non détenue : " + specialistId);
    }

    /**
     * Dépense un disque de la réserve — le coût générique d'une option de jeton de
     * plongée (ex. « défausser un disque → 5 recherche »), distinct du disque de
     * transit posé sur un site. Mutation en place.
     *
     * @throws IllegalArgumentException si la réserve est vide
     */
    public void spendReserveDisc() {
        if (reserveDiscs == 0) {
            throw new IllegalArgumentException("Aucun disque en réserve à dépenser");
        }
        reserveDiscs--;
    }

    /**
     * Les jetons de plongée actuellement en main, non encore dépensés — au plus un
     * seul doit y rester en fin de tour (règle vérifiée par le driver, pas ici).
     */
    public List<String> heldDiveTokens() {
        return Collections.unmodifiableList(heldDiveTokens);
    }

    /** Ajoute un jeton pris au sommet d'un site de plongée à la main du joueur. */
    public void receiveDiveToken(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Un jeton de plongée reçu doit avoir un identifiant");
        }
        heldDiveTokens.add(tokenId);
    }

    /**
     * Retire et renvoie le jeton en main à cet index (dépense d'une de ses options).
     *
     * @throws IndexOutOfBoundsException si l'index ne désigne aucun jeton en main
     */
    public String resolveDiveToken(int index) {
        return heldDiveTokens.remove(index);
    }

    /**
     * Les revues acquises par publication, dans l'ordre où elles ont été publiées
     * (comptées au décompte final pour leurs points imprimés).
     */
    public List<String> journals() {
        return Collections.unmodifiableList(journals);
    }

    /** Ajoute une revue tout juste publiée à la collection du joueur. */
    public void acquireJournal(String journalId) {
        if (journalId == null || journalId.isBlank()) {
            throw new IllegalArgumentException("Une revue acquise doit avoir un identifiant");
        }
        journals.add(journalId);
    }

    /** Copie indépendante, appelée une fois par simulation pour l'isoler (déc. 3). */
    public Player copy() {
        return new Player(attributes.copy(), reserveDiscs, transitDiscs, research, vesselStock,
                new ArrayList<>(specialists), new ArrayList<>(heldDiveTokens), new ArrayList<>(journals));
    }
}
