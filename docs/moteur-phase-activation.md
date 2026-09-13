# Moteur — Phase 2 : Activation (tours de jeu)

Fiche de conception de la **deuxième phase du moteur de jeu**. Elle décrit la
structure d'un tour d'activation et la façon dont on la découpera — **sans
code** : elle sert de référence pour l'implémentation à venir, comme
[`moteur-phase-preparation.md`](moteur-phase-preparation.md) l'a fait pour la
Phase 1. Elle applique à un cas concret les décisions de
[`architecture.md`](architecture.md), surtout la décision 1 (la signature
centrale), la décision 2 (état complet vs vue joueur) et la décision 4 (les
règles spéciales sont des données).

Statut : **conception** (carte 12 de l'Organiseur). Susceptible d'évoluer si
l'implémentation révèle un cas non prévu — d'autant que la synthèse des règles
n'explicite pas le découpage « 1 spécialiste + N revues » d'un tour, dégagé en
discussion.

## 1. Où se situe la Phase 2

Le vocabulaire, du plus large au plus fin : **partie › manche › phase › tour**.

- Une **partie** compte **6 manches**.
- Chaque **manche** = **Phase 1 (Préparation)** puis **Phase 2 (Activation)**.
- Un **tour** est le tour d'un joueur, *à l'intérieur* de la Phase 2.

La Phase 1 (fiche précédente) est **faite** : le moteur déroule premier joueur →
recrutement → cascade → effort → récupération, en s'arrêtant à chaque décision.
La Phase 2 reprend la main **après** la récupération (1c) de tous les joueurs.

## 2. La structure d'un tour

Principe directeur (décision 1) inchangé : le moteur **avance seul jusqu'à une
décision**, puis expose la **liste des coups légaux**. Mais là où la Phase 1
était une séquence figée, la Phase 2 est une **boucle ouverte** : chacun à son
tour, **jusqu'à ce que tous aient passé**.

Un **tour standard** se compose, dans l'ordre voulu par le joueur :

- l'**activation d'au plus un spécialiste** : poser un disque de la zone de
  transit sur sa **case d'activation libre**, puis exécuter tout ou partie de ses
  actions ;
- un **nombre libre d'actions de revue** : poser un disque sur une revue à action
  (ce disque **n'est jamais récupéré** en 1c) ;
- un **nombre libre de jetons Plongée** dépensés (voir §5).

Deux issues distinctes, à ne pas confondre :

| Coup | Effet |
|---|---|
| **Terminer le tour** | Rend la main ; le joueur **rejouera** ce round. |
| **Passer** | Quitte la manche **définitivement** : il ne rejoue plus cette Phase 2. |

La Phase 2 s'achève quand **tous** les joueurs ont passé. Tolérances de règle
utiles à modéliser :

- Activer un spécialiste **sans** exécuter ses actions est permis.
- **Aucune récompense n'est obligatoire.**
- Les jetons Plongée peuvent **remplacer entièrement** l'activation d'un
  spécialiste pour ce tour.

## 3. Le tour comme automate (décision 1)

Un tour n'est pas un coup unique mais une **suite de micro-coups sérialisables**,
tous des données (décision 1). L'état porte un **contexte de tour en cours** (quel
joueur, quel spécialiste vient d'être activé et où en est la chaîne de ses
actions, qui a déjà passé, la main de jetons du tour), copié avec le reste (déc.
2 et 3) pour que l'IA puisse cloner une partie en plein tour.

Les micro-coups pressentis :

| Coup | Contenu |
|---|---|
| `Activer(spécialiste)` | Pose un disque de transit sur la case d'activation libre du spécialiste. |
| `ExécuterAction(choix, params)` | Exécute **une** action de l'emplacement courant du spécialiste activé. |
| `ActionRevue(revue)` | Pose un disque sur une revue à action (disque définitif). |
| `DépenserJeton(jeton, option)` | Dépense un jeton Plongée : recherche **ou** effet spécial (jamais les deux). |
| `TerminerTour` | Clôt le tour (purge des jetons, cf. §5) ; le joueur reste dans la manche. |
| `Passer` | Clôt le tour et **sort le joueur de la manche**. |

**Enchaînement des actions d'un spécialiste** (déjà modélisé en données, cf.
[`architecture.md`] et la convention `actions`) : la face active porte une
`List<ActionSlot>`. Chaque emplacement est un **choix** (icônes séparées par
`/` → une seule alternative) ; la liste est l'**enchaînement** (icônes accolées →
tout ou partie, dans l'ordre voulu, chaque action **entièrement résolue** avant
la suivante). `ExécuterAction` avance dans cette chaîne ; le joueur peut s'arrêter
à tout moment (rien n'est obligatoire) et clore par `TerminerTour`.

**`legalActions` porte toutes les vérifications** : c'est le back qui dit, à
chaque instant, ce qui est jouable (submersible présent dans la zone, disque
disponible pour le coût, recherche suffisante, case libre…). Le front se contente
de **griser l'impossible en affichant le motif** (tooltip) — décision actée : le
front n'embarque aucune règle.

**Point à trancher à l'implémentation** : faut-il un `Activer` qui **inclut** le
premier choix d'action, ou un `Activer` nu suivi d'`ExécuterAction` ? La forme
« micro-coups séparés » est retenue par défaut (uniformité avec la cascade de la
Phase 1, génération de coups légaux simple), mais l'ergonomie de l'IA pourra la
faire réviser.

## 4. Les cinq actions

Toutes exigent un **submersible dans la zone visée**. Le détail de leur
**exécution** (l'océan jouable, le pathfinding, les piles) fait l'objet de cartes
sœurs (§9) ; ici on ne fige que leur **contrat**.

| Action | Coût | Effet (résumé) |
|---|---|---|
| **Voyage** | — | Déplacer un submersible ; portée et profondeur bornées par l'ingéniosité ; gagner le **bonus d'arrivée** de la zone de destination. |
| **Sonar** | +1 disque | Poser un disque sur la case libre la plus à gauche d'une piste Sonar : récompense, ou **découverte** d'une zone. |
| **Plongée** | — | Prendre le jeton du sommet d'un site de plongée. |
| **Conservation** | +1 disque | Payer un coût en recherche, poser un disque sur un site de conservation, gagner les récompenses. |
| **Publication** | +1 disque | Payer le coût d'une revue à l'étude, poser un disque sur un site de publication à l'icône voulue, acquérir la revue. |

Déplacement : de zone à zone, horizontal ou vertical, **sans franchir un vide** ;
le niveau de technologie borne **distance et profondeur**.

## 5. Jetons Plongée

- **Main de taille variable** pendant le tour : on peut en dépenser un nombre
  libre, **en plus** (ou à la place) de l'activation d'un spécialiste, **avant,
  après ou entre** ses actions.
- À la dépense, on choisit **la recherche OU l'effet spécial**, jamais les deux
  (schéma `options[]` de `dive-tokens.json`, cf. relevé).
- **Purge en fin de tour** : **un seul** jeton peut être conservé, sur
  l'emplacement de stockage dédié ; les autres **doivent** être dépensés avant de
  clore le tour. Le cas « 3 jetons et plus » est réel (un jeton `diveTokens: 2`
  plus un en stock) : `TerminerTour`/`Passer` ne sont **légaux** que si la main
  est ramenée à ≤ 1.

## 6. Mécanismes transverses

La plupart relèvent des cartes sœurs ; on les cadre pour que la structure du tour
les accueille sans se contredire.

- **Promotion.** C'est un **gain** (moteur d'effets) : retourner un Junior côté
  Senior, encaisser ses récompenses, **ouvrir une case d'activation Senior
  fraîche** (réactivable la même manche). Le **disque Junior est perdu** (renvoyé
  en réserve). Les icônes d'attribut du Junior sont conservées.
- **Découverte (via Sonar).** Piocher 2 tuiles, en garder 1, remettre l'autre
  **sous sa pile** (ni défaussée, ni retirée) ; la placer dans un emplacement
  valide de sa profondeur, encaisser le bonus d'arrivée. Information cachée :
  **masquée aux adversaires** via `PlayerView` (déc. 2).
- **Connexions.** Deux sites reliés dont les deux extrémités portent un disque :
  le joueur actif **et** le propriétaire de l'autre disque encaissent la
  récompense. Résolution joueur actif d'abord, puis sens horaire.
- **Plateau Impact.** Déjà modélisé (Phase 1) : pose sur départ ou voisin d'un
  hexagone occupé, cascade d'effets. Réutilisé tel quel.
- **Revues.** 24 standard + 8 de départ ; 4 « à l'étude » au début. Quatre
  catégories : bénéfice pour soi, bénéfice **pour les adversaires**, points de fin
  de partie, actions activables (disques **jamais récupérés**).

## 7. Répartition front / back

Inchangée (règle absolue de l'architecture) :

- **Back (Java)** = *tout le jeu* : contexte de tour, coups légaux, application,
  décompte. **Source unique de vérité.**
- **Front (Angular)** = *afficher + saisir* : il montre l'état visible et la liste
  des coups légaux, grise l'impossible avec son motif, renvoie le coup choisi.
  **Aucune règle dans le front.**

## 8. Sauvegarde & retour arrière

Inchangé : une partie = **graine + journal d'actions**, moteur **100 %
déterministe**. Les micro-coups de la Phase 2 entrent dans le journal comme ceux
de la Phase 1 ; le retour arrière reste gratuit. L'information cachée piochée
(sonar, jetons) ne se **re-tire pas** au rejeu — comportement sain.

## 9. Frontière : codable maintenant vs bloqué

- **Codable tout de suite** : la **structure du tour et la boucle de la Phase 2**
  — contexte de tour dans l'état, micro-coups `Activer` / `TerminerTour` /
  `Passer`, round-robin depuis le premier joueur jusqu'à ce que tous aient passé,
  purge des jetons en fin de tour. Testable en JUnit **sans** effets d'actions
  (activer = poser un disque ; l'exécution des actions vient après).
- **Bloqué sur des relevés/modèles absents** :
  - l'**exécution des cinq actions** attend un **océan jouable** (zones,
    profondeurs, sites, submersibles) et le pathfinding du Voyage ;
  - les **revues** — `journals.json` n'est pas chargé dans le moteur ;
  - les **jetons Plongée** — `dive-tokens.json` relevé mais pas encore chargé
    côté moteur ;
  - la **promotion** dépend du gain `PROMOTE` (aujourd'hui `GainResolver` lève
    une exception pour les gains méta).

Conséquence pratique, comme en Phase 1 : la Phase 2 peut tourner « à blanc » très
tôt (activer/finir/passer, sans effet d'action), puis se compléter action par
action.

## 10. Découpage en cartes sœurs

La carte 12 ne couvre que **la structure du tour + la boucle**. Le reste, à
planifier séparément :

1. **Façade `Game`** : enchaîner Phase 1 → Phase 2 → manche suivante, et la fin de
   partie après la manche 6. Généralise `legalActions`/`apply` par dispatch de
   phase (le curseur de préparation et le contexte de tour sont prêts à
   l'accueillir).
2. **Moteur d'effets & bus d'événements** (décision 4) : `TokenClaimed`,
   `VesselDeparted`, `SonarPlaced`, `SiteFilled`, `Conserved`…
3. **Océan jouable + Voyage** (zones, profondeurs, sites, submersibles,
   pathfinding).
4. **Découverte Sonar** (piles de tuiles, pioche 2 / garde 1, placement).
5. **Chargeur & modèle des revues** (`journals.json`).
6. **Chargeur & modèle des jetons Plongée** (`dive-tokens.json`).
7. **Boucle de partie & décompte final** (les cinq postes du décompte + objectifs
   de mission).

## Points encore ouverts

- Granularité `Activer` vs `ExécuterAction` (cf. §3) — tranché par défaut en
  micro-coups séparés, à confirmer à l'implémentation.
- Forme exacte du **contexte de tour** dans `GameState` (analogue du
  `PreparationCursor`).
- Ordre de résolution fin des **connexions** et des effets déclenchés simultanés —
  à préciser sur cas réels avec le bus d'événements.
