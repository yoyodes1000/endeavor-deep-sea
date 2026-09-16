package io.github.yoyodes1000.endeavor.app.dive;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yoyodes1000.endeavor.engine.dive.DiveOption;
import io.github.yoyodes1000.endeavor.engine.dive.DiveToken;
import io.github.yoyodes1000.endeavor.engine.dive.DiveTokenCatalog;
import io.github.yoyodes1000.endeavor.engine.specialist.ActionType;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.OptionalInt;

/**
 * Charge le catalogue des jetons de plongée depuis le JSON du matériel et le
 * traduit vers le modèle du moteur. Même partage que les autres chargeurs :
 * Jackson lit la forme, le moteur valide le vocabulaire ({@code fromCode}) et la
 * sémantique (constructeurs). Chargeur agnostique de l'I/O.
 */
public final class DiveTokenLoader {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    /**
     * Lit et valide le catalogue.
     *
     * @throws IllegalArgumentException si une donnée est incohérente (vocabulaire
     *     inconnu, option mélangeant gains et action, identifiant en double…)
     * @throws UncheckedIOException si le flux est illisible ou mal formé
     */
    public DiveTokenCatalog load(Reader source) {
        DiveTokensDocument document = read(source);
        if (document == null || document.diveTokens() == null) {
            throw new IllegalArgumentException("JSON des jetons de plongée vide ou sans tableau « diveTokens »");
        }
        List<DiveToken> tokens = document.diveTokens().stream()
                .map(DiveTokenLoader::toToken)
                .toList();
        return new DiveTokenCatalog(tokens);
    }

    private DiveTokensDocument read(Reader source) {
        try {
            return mapper.readValue(source, DiveTokensDocument.class);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON des jetons de plongée illisible", e);
        }
    }

    private static DiveToken toToken(DiveTokensDocument.Entry entry) {
        if (entry.copies() == null) {
            throw new IllegalArgumentException("Nombre d'exemplaires manquant pour le jeton " + entry.id());
        }
        if (entry.options() == null || entry.options().isEmpty()) {
            throw new IllegalArgumentException("Le jeton " + entry.id() + " doit avoir des options");
        }
        List<DiveOption> options = entry.options().stream().map(DiveTokenLoader::toOption).toList();
        return new DiveToken(entry.id(), entry.copies(), options);
    }

    private static DiveOption toOption(DiveTokensDocument.Option option) {
        if (option.action() != null) {
            if (option.gains() != null || option.cost() != null) {
                throw new IllegalArgumentException("Une option de jeton ne mélange pas gains et action");
            }
            ActionType type = ActionType.fromCode(option.action().type());
            OptionalInt costModifier = option.action().costModifier() == null
                    ? OptionalInt.empty()
                    : OptionalInt.of(option.action().costModifier());
            return new DiveOption.TriggersAction(type, costModifier);
        }
        return new DiveOption.Gains(gains(option.gains()), gains(option.cost()));
    }

    private static List<Gain> gains(List<String> raw) {
        return raw == null ? List.of() : raw.stream().map(Gain::fromCode).toList();
    }
}
