# Moteur — Phase 1 : Préparation (début de manche)

Fiche de conception de la **première phase du moteur de jeu**. Elle décrit la
séquence exacte de la Phase 1 et la façon dont on la découpera — **sans code** :
elle sert de référence pour l'implémentation à venir. Elle applique à un cas
concret les décisions de [`architecture.md`](architecture.md), surtout la
décision 1 (la signature centrale), la décision 2 (état complet vs vue joueur)
et la section Sauvegarde.

Statut : **conception** (carte 11 de l'Organiseur). Susceptible d'évoluer si
l'implémentation révèle un cas non prévu.

## 1. Où se situe la Phase 1

Le vocabulaire, du plus large au plus fin : **partie › manche › phase › tour**.

- Une **partie** compte **6 manches**.
- Chaque **manche** = **Phase 1 (Préparation)** puis **Phase 2 (Activation)**.
- Un **tour** est le tour d'un joueur, *à l'intérieur* de la Phase 2.

Cette fiche couvre la **Phase 1**. Les tours d'activation (Phase 2) feront
l'objet de la carte 12.

## 2. Séquence exacte

Principe directeur (décision 1 de l'architecture) : le moteur **avance seul
jusqu'à ce qu'il ait besoin d'une décision du joueur**, puis s'arrête et expose
la **liste des coups légaux**. La Phase 1 comporte ainsi **deux points de
décision** et **deux étapes automatiques**.

| Étape | Type | Contenu |
|---|---|---|
| Premier joueur | automatique | Manche 1 : tirage aléatoire ; manches suivantes : rotation horaire |
| 1a Recrutement | **décision** | Choisir une tuile spécialiste, puis résoudre ses gains |
| 1b Effort | automatique | Gagner des disques selon l'inspiration |
| 1c Récupération | **décision** | Choisir sur quels spécialistes reprendre ses disques |

Chaque joueur enchaîne 1a → 1b → 1c **dans l'ordre du tour**. L'ordre 1a → 1b →
1c est *mécanique*, pas décoratif : voir la cascade des gains en 1a.

### Premier joueur (automatique)

- **Manche 1** : détermination **aléatoire**, via la **graine injectée** — jamais
  d'appel implicite au hasard (impératif de déterminisme, cf. Sauvegarde).
- **Manches 2 à 6** : le rôle **passe d'un cran dans le sens horaire** au début de
  chaque nouvelle manche.
- L'état porte l'**index du premier joueur**, avancé à chaque nouvelle manche.
  L'ordre du tour de toute la manche (Phases 1 **et** 2) en découle.
- Vérifié sur le livret : c'est bien une rotation simple.

### 1a — Recrutement (décision)

- Chaque joueur, dans l'ordre du tour, recrute **une** tuile spécialiste du
  **casier partagé**.
- Contrainte : **rang ≤ niveau de réputation**. Toujours pris **côté Junior**.
- Ses **gains se résolvent immédiatement**, dans l'**ordre choisi par le joueur**,
  et **peuvent s'enchaîner** (cascade). Exemple : un gain fait monter
  l'ingéniosité → en atteignant la case 2 ou 7 on gagne un *vessel* (→ zone de
  lancement) ; et toute montée d'**inspiration** ou de **coordination** profite
  **dès la même manche** en 1b/1c.
- Le casier étant **partagé**, l'**ordre du tour compte ici** : le premier à
  recruter a le choix le plus large.
- C'est **l'étape la plus riche** : « résoudre les gains » est un véritable petit
  **moteur d'effets** (décision 4 : les gains décrits en données), et c'est le
  **seul** des trois morceaux qui dépend du modèle des spécialistes.

### 1b — Effort (automatique)

- Chaque joueur prend **N disques** de la réserve vers la **zone de transit**,
  avec **N = niveau d'inspiration**.
- Aucun choix : entièrement automatique.

### 1c — Récupération (décision)

- Chaque joueur reprend des disques posés sur ses spécialistes vers la **zone de
  transit**, autant que son **niveau de coordination**.
- **Le joueur choisit sur quels spécialistes** il les reprend : c'est un **vrai
  choix** (contrairement à 1b).
- La reprise **libère** ces spécialistes (réactivables en Phase 2).
- Les disques posés sur des **revues** ne se reprennent **jamais**.
- **Cas particulier de la manche 1** : aucun disque n'est encore posé sur un
  spécialiste, donc **1c ne fait rien**.

### Invariants utiles

- En fin de partie, chaque joueur a exactement **7 spécialistes** : le chef
  d'équipe + 6 recrutés (un par manche).
- Donc **à la fin de la Phase 1 de la manche 1**, chaque joueur a **2
  spécialistes** (chef + 1 recruté).

## 3. Répartition front / back

**Règle absolue** (architecture) : le moteur ne connaît ni l'interface, ni le
réseau. La conséquence sur la Phase 1 :

- **Back (Java)** = *tout le jeu* : l'état, les coups légaux, l'application, le
  décompte — et plus tard l'IA. **Source unique de vérité.**
- **Front (Angular)** = *afficher + saisir* : il montre l'état visible du joueur
  et la **liste des coups légaux calculée par le back**, capture le clic, renvoie
  le coup choisi. **Aucune règle dans le front** (sinon deux vérités qui
  divergent).

Exemple, le recrutement : le back dit « recrutables : X, Y, Z » → le front
affiche le casier avec X/Y/Z cliquables → le joueur clique Y → le front envoie
le coup `Recruter(Y)` → le back applique, résout les gains, renvoie le nouvel
état. Le front n'a jamais décidé *qui* était recrutable.

**Les coups de la Phase 1** (des données sérialisables, décision 1) :

- `Recruter(spécialiste)` — étape 1a.
- `Récupérer(spécialiste)` — étape 1c (un coup par disque repris, ou un coup
  portant la liste des spécialistes choisis — à trancher à l'implémentation).
- Le premier joueur et l'effort sont **automatiques** : ce ne sont pas des coups.

## 4. Sauvegarde & retour arrière

**Format** (décision Sauvegarde de l'architecture) : une partie sauvegardée =
**une graine aléatoire + le journal des actions**. Pas d'instantané d'état.

- Le **retour arrière est gratuit** : on retire la ou les dernières actions et on
  **rejoue** depuis la graine.
- Rejouer donne une partie **identique** (même graine → mêmes tirages) : l'undo
  **ne re-tire pas** l'information cachée. C'est un comportement sain.
- Corollaire obligatoire : le moteur est **100 % déterministe** (graine injectée,
  aucun hasard implicite).

**Emplacement et cycle de vie.** Sauvegarde **locale** (pas sur Drive) : un
fichier unique pour la partie en cours, écrasé à chaque nouvelle partie, plus une
**archive de debug plafonnée** pour le rejeu de l'IA. Le détail canonique vit
dans [`architecture.md`](architecture.md) (section Sauvegarde) ; c'est un point du
module `app`, **hors du code de la Phase 1**.

## 5. Frontière : codable maintenant vs bloqué

- **Codable tout de suite** : le premier joueur (graine + rotation), l'effort
  (1b) et la récupération (1c) — logique simple sur les pistes, plus un choix
  pour 1c. Testables en JUnit **sans** données de spécialistes.
- **Bloqué sur le relevé/chargeur des spécialistes** : la **résolution des gains
  en 1a**. On peut poser le coup `Recruter` et l'ossature de l'étape, mais la
  résolution des gains attend le **modèle des spécialistes** (leurs rangs et
  gains, chargés depuis `specialists.json` avec validation stricte — étape 2 de
  l'ordre de travail de l'architecture).

Conséquence pratique : la Phase 1 peut tourner « à blanc » très tôt (recrutement
sans effet de gains), et se compléter dès que les spécialistes seront chargés.
