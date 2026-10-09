package io.github.yoyodes1000.endeavor.app.game;

/**
 * Une demande de nouvelle partie. Les champs sont nullables pour qu'un champ absent
 * soit refusé explicitement par {@link GameService}, plutôt que lu comme zéro.
 *
 * @param mission   le numéro de la mission à jouer
 * @param opponents le nombre d'adversaires artificiels
 * @param seed      la graine de la partie, facultative : tirée au hasard si absente,
 *                  imposée pour rejouer une partie à l'identique
 */
public record NewGameRequest(Integer mission, Integer opponents, Long seed) {
}
