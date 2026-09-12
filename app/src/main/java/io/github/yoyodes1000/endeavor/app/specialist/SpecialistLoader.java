package io.github.yoyodes1000.endeavor.app.specialist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionSlot;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameCount;
import io.github.yoyodes1000.endeavor.engine.specialist.EndGameScoring;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialAbility;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Charge le casier des spécialistes depuis le JSON du matériel et le traduit
 * vers le modèle du moteur.
 *
 * <p>Le partage des responsabilités suit le découpage de l'architecture : c'est
 * ici, dans {@code app}, que vit Jackson (le moteur reste sans dépendance). Jackson
 * lit la forme du document ; le moteur valide le vocabulaire (les
 * {@code fromCode}) et la sémantique (les constructeurs de son modèle). Aucune
 * incohérence ne passe en silence : elle interrompt le chargement.
 *
 * <p>Le chargeur est volontairement agnostique de l'entrée/sortie : il lit un
 * {@link Reader}, ce qui le rend testable sans fichier. Savoir <em>où</em> se
 * trouve le fichier réel est un souci de câblage, laissé à l'appelant.
 */
public final class SpecialistLoader {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    /**
     * Lit et valide le casier complet.
     *
     * @throws IllegalArgumentException si une donnée est incohérente (vocabulaire
     *     inconnu, rang invalide, décompte fantôme, identifiant en double…)
     * @throws UncheckedIOException si le flux est illisible ou mal formé
     */
    public SpecialistRoster load(Reader source) {
        SpecialistsDocument document = read(source);
        if (document == null || document.specialists() == null) {
            throw new IllegalArgumentException(
                    "JSON des spécialistes vide ou sans tableau « specialists »");
        }
        List<Specialist> specialists = document.specialists().stream()
                .map(SpecialistLoader::toSpecialist)
                .toList();
        return new SpecialistRoster(specialists);
    }

    private SpecialistsDocument read(Reader source) {
        try {
            return mapper.readValue(source, SpecialistsDocument.class);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON des spécialistes illisible", e);
        }
    }

    private static Specialist toSpecialist(SpecialistsDocument.Entry entry) {
        boolean teamLeader = Boolean.TRUE.equals(entry.teamLeader());
        OptionalInt rank = entry.rank() == null
                ? OptionalInt.empty()
                : OptionalInt.of(entry.rank());
        return new Specialist(entry.id(), rank, teamLeader,
                toSide(entry.junior()), toSide(entry.senior()));
    }

    private static SpecialistSide toSide(SpecialistsDocument.Side side) {
        if (side == null) {
            throw new IllegalArgumentException("Une face (Junior/Senior) est manquante");
        }
        List<Gain> gains = side.immediateGains() == null
                ? List.of()
                : side.immediateGains().stream().map(Gain::fromCode).toList();
        List<ActionSlot> actions = side.actions() == null
                ? List.of()
                : side.actions().stream().map(SpecialistLoader::toSlot).toList();
        Optional<SpecialAbility> ability = side.specialAbility() == null
                ? Optional.empty()
                : Optional.of(new SpecialAbility(side.specialAbility().text()));
        Optional<EndGameScoring> endGame = side.endGameScoring() == null
                ? Optional.empty()
                : Optional.of(toEndGame(side.endGameScoring()));
        return new SpecialistSide(side.name(), gains, actions, ability, endGame);
    }

    private static ActionSlot toSlot(List<String> choices) {
        if (choices == null) {
            throw new IllegalArgumentException("Un emplacement d'action est nul");
        }
        return new ActionSlot(choices.stream().map(ActionType::fromCode).toList());
    }

    private static EndGameScoring toEndGame(SpecialistsDocument.EndGameScoring scoring) {
        if (scoring.points() == null || scoring.per() == null) {
            throw new IllegalArgumentException(
                    "Un décompte de fin de partie doit préciser « points » et « per »");
        }
        return new EndGameScoring(
                scoring.text(), scoring.points(), scoring.per(), EndGameCount.fromCode(scoring.count()));
    }
}
