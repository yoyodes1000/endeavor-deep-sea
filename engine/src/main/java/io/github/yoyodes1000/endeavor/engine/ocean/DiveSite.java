package io.github.yoyodes1000.endeavor.engine.ocean;

/**
 * Un site de plongée d'une tuile : à la pose de la zone (mise en place ou
 * découverte), on y empile {@code tokenCount} jetons de plongée tirés au hasard —
 * l'action Plongée en prend ensuite le sommet.
 *
 * <p>Matériel immuable : le site ne décrit que le nombre de jetons à empiler à la
 * création de la zone. L'empilement réel (quels jetons, dans quel ordre) est de
 * l'état mutable, porté par {@link OceanBoard}.
 *
 * @param id        l'identifiant du site sur sa tuile (unique par tuile)
 * @param tokenCount le nombre de jetons empilés à la pose de la zone
 */
public record DiveSite(String id, int tokenCount) {

    public DiveSite {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un site de plongée doit avoir un identifiant");
        }
        if (tokenCount < 1) {
            throw new IllegalArgumentException("Un site de plongée empile au moins un jeton : " + tokenCount);
        }
    }
}
