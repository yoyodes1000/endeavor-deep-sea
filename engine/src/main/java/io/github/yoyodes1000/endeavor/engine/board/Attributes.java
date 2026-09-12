package io.github.yoyodes1000.endeavor.engine.board;

import java.util.EnumMap;
import java.util.Map;

/**
 * Les quatre pistes d'attribut d'un joueur, réunies.
 *
 * <p>Contrairement aux records immuables qui l'entourent, c'est un objet d'état
 * <strong>mutable</strong> (décision 3) : la recherche Monte-Carlo copie l'état
 * des milliers de fois par seconde et applique les coups <em>en place</em>, sans
 * réallouer. En contrepartie, une discipline stricte — personne hors du moteur ne
 * mute un état — et une {@link #copy() copie} explicite qui isole chaque
 * simulation. Les valeurs portées ({@link AttributeTrack}) restent, elles,
 * immuables : la copie ne recopie que la table.
 */
public final class Attributes {

    private final Map<Attribute, AttributeTrack> tracks;

    private Attributes(Map<Attribute, AttributeTrack> tracks) {
        this.tracks = tracks;
    }

    /** Les quatre pistes au départ de la partie : chacune à la case 0 (niveau 1). */
    public static Attributes atStart() {
        Map<Attribute, AttributeTrack> tracks = new EnumMap<>(Attribute.class);
        for (Attribute attribute : Attribute.values()) {
            tracks.put(attribute, AttributeTrack.start());
        }
        return new Attributes(tracks);
    }

    public AttributeTrack track(Attribute attribute) {
        return tracks.get(attribute);
    }

    public int step(Attribute attribute) {
        return track(attribute).step();
    }

    public int level(Attribute attribute) {
        return track(attribute).level();
    }

    /** Avance une piste de {@code steps} cases — mutation en place. */
    public void advance(Attribute attribute, int steps) {
        tracks.put(attribute, track(attribute).advancedBy(steps));
    }

    /** Total des points des quatre pistes (pour le décompte final). */
    public int totalPoints() {
        int total = 0;
        for (AttributeTrack track : tracks.values()) {
            total += track.points();
        }
        return total;
    }

    /** Copie indépendante, appelée une fois par simulation pour l'isoler (déc. 3). */
    public Attributes copy() {
        return new Attributes(new EnumMap<>(tracks));
    }
}
