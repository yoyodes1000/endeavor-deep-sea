# Moteur — Océan jouable & Voyage

Fiche de conception de l'**océan jouable** et de la première des cinq actions,
le **Voyage**. Comme [`moteur-phase-activation.md`](moteur-phase-activation.md)
et [`moteur-phase-preparation.md`](moteur-phase-preparation.md), elle décrit le
modèle et le découpage **sans code**. Elle applique les décisions
d'[`architecture.md`](architecture.md) — surtout la 3 (état mutable + `copy()`),
la 4 (règles spéciales en données), la 7 (adjacence *calculée*, jamais stockée)
et la 8 (`setup` : la grille de l'océan est une donnée de mission).

Statut : **conception**. Le modèle est celui de la mission 1 ; les cas des
scénarios tardifs (verrouillage de profondeur, deux grilles, grottes) sont
identifiés comme différés, pas encore éprouvés.

## 1. Où se situe ce chantier

C'est la **carte 3 du découpage** de la fiche d'activation (§10) : l'océan
jouable. Aujourd'hui la Phase 2 tourne « à blanc » — `Activer` / `TerminerTour` /
`Passer` — mais **aucune des cinq actions n'agit**, car toutes exigent un
support commun absent : un océan où des submersibles se déplacent. Le donner
**débloque les quatre autres actions**, qui viendront se brancher sur la même
boucle.

La fiche couvre donc : le **modèle de l'océan**, les **submersibles**, le
**Voyage** (pathfinding + coup), et la **boucle d'exécution d'action** dont le
Voyage est le premier client. Les autres actions, la découverte, le décompte et
les règles spéciales de zones restent hors périmètre.

## 2. L'océan est une grille

L'océan est une **grille `profondeur × colonnes`** :

- **Profondeur** de 1 (surface) à 5 (abysses) — les cinq niveaux du glossaire.
- **Colonnes** A, B, C… en nombre fixé par la mission (`setup.columns` = 5 pour
  la mission 1).
- Chaque **case** porte **au plus une zone** (une tuile Océan en jeu). Des cases
  restent **vides** : la grille de départ est éparse (mission 1 : B1, C1, C2, D1
  seulement).
- La grille **grandit en cours de partie** : la découverte (via Sonar) place de
  nouvelles tuiles, toujours **sous** une zone existante (hors profondeur 1).
  Ce chantier prépare le terrain ; la découverte elle-même est une carte sœur.

## 3. Deux couches : matériel immuable, état mutable

Exactement le patron du plateau Impact ([`MissionBoard`], décisions 3 et 7) :

- **Matériel immuable** : le catalogue des tuiles (`OceanTile` /
  `OceanTileCatalog`, déjà chargé). Le Voyage n'y lit que ce qui existe déjà —
  `depth`, `arrivalBonus` — donc **`OceanTile` n'est pas étendu** ici. Les sites
  (`diveSites`, `sonarTracks`…) viendront **au fil des actions** qui les
  exploitent (choix acté).
- **État mutable** : un nouvel `OceanBoard`, copié une fois par simulation
  (`copy()`), qui porte ce qui évolue :
  - le **placement** — quelle tuile occupe chaque case, `Cell(depth, col) →
    tileId` ;
  - les **submersibles en jeu** — combien, de chaque joueur, dans chaque zone
    (une zone peut en porter plusieurs).

L'**occupation des sites** par des disques (conservation, sonar, publication) et
les piles de jetons de plongée **ne sont pas modélisées ici** : rien dans le
Voyage n'y touche, et elles arriveront avec les actions qui les écrivent (YAGNI).

### Coordonnées

`Cell(depth, col)` : `depth` de 1 à 5, `col` en **entier 0-based** dans le
moteur. Les colonnes sont des **lettres dans les données** (A, B…) ; la
conversion lettre ↔ index se fait à la **frontière du chargeur** (`app`), le
moteur restant purement numérique — même principe que partout : la donnée est
lisible par un humain, le moteur calcule sur des nombres.

### Adjacence — calculée, jamais stockée (décision 7)

Les **voisins** d'une case sont les **quatre cases orthogonales** `(depth±1,
col)` et `(depth, col±1)` **qui portent une tuile**. Une case vide n'est pas un
voisin : c'est ce qui interdit de « franchir un vide ». L'adjacence se calcule à
la demande depuis le placement ; on ne stocke aucune table d'arêtes (une source
d'erreur en moins, et l'IA parcourt ce graphe des milliers de fois par seconde).

## 4. Les submersibles

Le submersible est le pion qui rend une zone **agissable** : les cinq actions
exigent un submersible dans la zone visée.

- **Stock du joueur.** `Player` gagne un champ `vesselStock` : les submersibles
  **non déployés**, hors grille. C'est le pendant de la réserve de disques. Le
  **nombre de départ** est un paramètre de mise en place (comme `startingDiscs`),
  à fixer avec les données de mission.
- **Submersibles en jeu.** Portés par l'`OceanBoard`, par zone et par joueur.
- **Mise en jeu au démarrage.** Chaque mission désigne une **base d'opérations**
  (`baseOfOperations`, terme du glossaire). **Au début de la partie, chaque
  joueur pose un submersible de son stock sur la base d'opérations et encaisse
  son bonus d'arrivée**, avant le premier recrutement. Pour la mission 1 la base
  est **`the-sea-star`** (C1) et son bonus d'arrivée est `["disc"]` : la pose est
  donc **entièrement automatique** (aucune décision, aucun choix de gain).
- **Submersibles gagnés en jeu.** Atteindre la case 2 ou 7 d'ingéniosité gagne un
  submersible. Le `GainResolver` **détecte déjà** ces gains
  (`EffectOutcome.vessels`) mais **ne les pose pas** — exactement comme les
  impacts. Ce chantier leur donne enfin un foyer : le submersible gagné rejoint
  le **stock**. Sa **mise en jeu** (depuis le stock vers une zone) est un pas de
  pose, analogue à `PoserImpact` — sa forme exacte est un **point ouvert**
  (cf. fin de fiche).

## 5. Le Voyage

Déplacer un submersible d'une zone à une autre, puis encaisser le **bonus
d'arrivée de la seule destination**.

- **Chemin** : de proche en proche entre zones **adjacentes occupées** (§3),
  horizontal ou vertical, sans franchir un vide.
- **Portée bornée par le niveau de technologie** (ingéniosité) : la **distance**
  (nombre de pas du chemin) **et** la **profondeur** atteinte sont toutes deux
  ≤ niveau. Rappel décision 5 : le **niveau est la valeur** — au niveau 3, on va
  jusqu'à 3 cases de distance et jusqu'à la profondeur 3.
- **Pathfinding** : un **parcours en largeur borné** (`reachableFrom(cell,
  niveau)`) sur le graphe des cases occupées, renvoyant l'ensemble des
  destinations légales. C'est une **requête pure** de l'`OceanBoard`, sans effet
  de bord — testable seule, et réutilisable par l'IA.
- **Le coup** : `Voyager(submersible, destination)`. `legalActions` énumère, pour
  chaque zone où le joueur a un submersible, les destinations accessibles ;
  `apply` déplace le submersible et résout le bonus d'arrivée (le `GainResolver`
  sait déjà le faire).

**Différé — les règles spéciales de zones** qui modifient le déplacement sont des
**effets déclenchés** (décision 4, bus d'événements encore absent), pas des `if`
dans le pathfinding : Vortex (`voyage sortant` : +1 en distance **et**
profondeur), Black Smoker (interdit d'aller vers la profondeur 5), Submerged Cave
System (aucun submersible n'entre ni ne traverse), Abyssal Trench (un seul
submersible par joueur). Le pathfinding de base les ignore ; elles se
brancheront dessus quand le bus existera.

## 6. La boucle d'exécution d'une action

Le Voyage est la **première action concrète** : il oblige à construire la boucle
d'exécution que la fiche d'activation avait laissée en attente (§3, §9). Choisi
comme premier parce qu'il est le plus simple — **ni coût en disque, ni
recherche**.

Aujourd'hui l'`ActivationDriver` s'arrête après `Activer` : le tour ne sait pas
« exécuter les actions du spécialiste activé ». On ajoute :

- Le **contexte de tour** s'enrichit (dans l'`ActivationCursor`, immuable et
  copié) : quel **spécialiste** vient d'être activé, et **où en est la chaîne de
  ses actions** (l'indice de l'emplacement courant). Rappel
  [[actions-encoding]] : la face active porte un tableau d'`ActionSlot` —
  l'**extérieur** est l'enchaînement (tout ou partie, dans l'ordre), l'**intérieur**
  le choix (une alternative de l'emplacement).
- Après `Activer`, `legalActions` propose les **coups d'action** de l'emplacement
  courant (pour un emplacement `travel`, les `Voyager` légaux) **plus**
  `TerminerTour` / `Passer` — car **rien n'est obligatoire** : on peut activer
  sans agir, ou s'arrêter à tout moment.

**Granularité — proposition à confirmer.** Plutôt qu'un `ExécuterAction(choix,
params)` générique (esquissé dans la fiche d'activation), on introduit des
**coups concrets et typés** — `Voyager`, puis un par action — chacun n'étant
**légal que lorsque le contexte de tour attend ce type d'action**. C'est plus
proche du `sealed Action` existant (`Recruter`, `PoserImpact`, `Activer`…) et
rend le `switch` exhaustif vérifiable par le compilateur. La sélection du
**choix** d'un emplacement se fait en jouant tel ou tel coup concret ; l'avance
dans la **chaîne** se fait après chaque action entièrement résolue.

## 7. Répartition front / back

Inchangée (règle absolue). Le **back** porte tout : placement, adjacence,
pathfinding, contexte de tour, coups légaux, application. Le **front** affiche
l'océan et la liste des Voyages légaux, grise l'impossible avec son motif
(« hors de portée », « aucun submersible ici »), et renvoie le coup choisi.
Aucune règle de déplacement dans le front.

## 8. Déterminisme

La grille de départ peut comporter des tirages (`{depth, col, randomLevel}` :
une tuile au hasard dans la pile d'un niveau). Ce tirage passe par la
**`RandomSource` injectée** (jamais d'aléa implicite) : la même graine rebâtit le
même océan. Les Voyages entrent dans le journal d'actions comme le reste ; le
retour arrière reste gratuit.

## 9. Codable maintenant vs différé

**Codable tout de suite :**

- l'`OceanBoard` : placement depuis `setup` (colonnes, `startingTiles`, tirage
  `randomLevel`), submersibles par zone/joueur, `copy()` ;
- l'**adjacence calculée** et le **pathfinding** borné (requête pure) ;
- le **stock de submersibles** sur `Player` et la **pose au démarrage** sur la
  base d'opérations + bonus d'arrivée ;
- la **boucle d'exécution** et le coup **`Voyager`** de bout en bout.

**Différé (dépend de relevés/systèmes absents) :**

- les **règles spéciales de zones** qui modifient le Voyage (bus d'événements,
  décision 4) ;
- la **découverte Sonar** qui fait grandir la grille (carte sœur) ;
- l'**occupation des sites** par des disques (viendra avec conservation / sonar /
  publication) ;
- la **mise en jeu des submersibles gagnés** depuis le stock (point ouvert) ;
- le **décompte final** (les submersibles en zone comptent pour certains
  objectifs et décomptes Senior).

## 10. Découpage en deux cartes

**Carte A — Océan jouable (modèle pur).** L'`OceanBoard` (placement +
submersibles + `copy()`), sa construction depuis le `setup` de mission,
l'adjacence calculée et le pathfinding borné comme **requête pure**, le champ
`vesselStock` sur `Player`. **Aucune** intégration au tour : testable en JUnit
comme le fut le plateau Impact — on bâtit un océan, on interroge voisins et
destinations, on vérifie les bornes (distance, profondeur, vide infranchissable).

**Carte B — Voyage jouable (intégration).** La **pose du premier submersible au
démarrage** (base d'opérations + bonus d'arrivée), la **boucle d'exécution**
(contexte de tour enrichi, coups d'action après `Activer`), et le coup
**`Voyager`** avec ses coups légaux et son application. Testable de bout en bout :
activer un spécialiste `travel`, voyager vers une zone accessible, encaisser le
bonus d'arrivée ; vérifier qu'un Voyage hors portée est refusé.

Chaque carte = une branche `feature/…` et une PR vers `developpement`, revue
brique par brique.

## Points ouverts

- **Mise en jeu des submersibles gagnés** (ingéniosité 2/7) : le gain rejoint le
  stock, mais la pose depuis le stock vers une zone reste à concevoir — pas
  analogue à `PoserImpact` (une décision et une éventuelle cascade), à trancher
  sur cas réel.
- **Données de mission à ajouter** (sous confirmation, comme toute écriture) :
  `setup.baseOfOperations` dans `missions.json`, et le **nombre de submersibles
  de départ** par joueur.
- **Granularité des coups d'action** (§6) : coups concrets typés (`Voyager`…)
  proposés par défaut, à confirmer à l'implémentation.
- **Forme exacte du contexte de tour** dans l'`ActivationCursor` (spécialiste
  activé + avancement dans la chaîne) — à figer en écrivant la carte B.
- **Profondeur d'un submersible déjà plus profond que le niveau** : cas de bord
  du pathfinding (peut-il toujours remonter ?), à préciser sur cas réel.
