/**
 * Les joueurs artificiels.
 *
 * <p>Pour l'instant, un seul : {@link io.github.yoyodes1000.endeavor.ai.RandomBot},
 * qui joue au hasard parmi les coups légaux. La recherche arborescente Monte-Carlo
 * avec déterminisation viendra ensuite, derrière la même interface
 * {@link io.github.yoyodes1000.endeavor.ai.Bot}.
 *
 * <p>Contrainte à respecter dès la première classe : l'IA ne reçoit qu'une vue
 * partielle de la partie, jamais l'état complet. C'est ce qui l'empêche
 * mécaniquement de connaître les piles face cachée.
 */
package io.github.yoyodes1000.endeavor.ai;
