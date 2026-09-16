package io.github.yoyodes1000.endeavor.engine.dive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * L'ensemble des types de jetons de plongée du matériel. Le constructeur
 * garantit l'unicité des identifiants — comme pour le reste du modèle, une
 * incohérence fait échouer le chargement.
 */
public record DiveTokenCatalog(List<DiveToken> tokens) {

    public DiveTokenCatalog {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Le catalogue de jetons de plongée est vide");
        }
        tokens = List.copyOf(tokens);

        Map<String, DiveToken> byId = new LinkedHashMap<>();
        for (DiveToken token : tokens) {
            if (byId.put(token.id(), token) != null) {
                throw new IllegalArgumentException("Identifiant de jeton de plongée en double : " + token.id());
            }
        }
    }

    /** Le jeton portant cet identifiant, si le catalogue en connaît un. */
    public Optional<DiveToken> byId(String id) {
        return tokens.stream().filter(token -> token.id().equals(id)).findFirst();
    }
}
