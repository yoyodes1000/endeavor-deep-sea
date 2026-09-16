package io.github.yoyodes1000.endeavor.engine.action;

/**
 * Un coup du jeu : une <strong>donnée sérialisable</strong>, pas un appel de
 * méthode (décision 1). Un type et ses paramètres, que l'interface affiche,
 * l'IA explore, les tests rejouent et la sauvegarde enregistre.
 *
 * <p>Interface scellée : la liste des coups possibles est fermée et connue du
 * compilateur, ce qui rend leur traitement exhaustif vérifiable.
 */
public sealed interface Action permits Recruter, PoserImpact, Recuperer, Passer, Activer, TerminerTour, Voyager,
        Sonar, GarderTuile, PoserTuile, Dive, DepenserJeton, Conserver, Publier, Promouvoir {
}
