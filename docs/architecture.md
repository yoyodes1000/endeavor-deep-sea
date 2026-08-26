# Architecture

Décisions structurantes du projet et leurs justifications. Ce document est fait
pour être contesté : si une décision se révèle mauvaise, on la remplace ici, en
gardant trace du pourquoi.

## Contexte

Adaptation jouable en solo d'*Endeavor: Deep Sea*, en mode compétitif contre 1
à 3 adversaires artificiels, chacun réglable parmi quatre niveaux de difficulté.
Application locale, sans compte ni réseau.

Deux contraintes dominent, tirées de la synthèse des règles (`docs/prive/`,
non versionné) :

1. **Le volume et l'irrégularité des règles.** 37 tuiles Océan, 21 spécialistes
   recto-verso, 32 revues, 36 jetons, et surtout 10 scénarios dont le plateau
   Impact n'est pas un mécanisme paramétré mais dix mécanismes distincts.
2. **L'information cachée.** Piles de tuiles, jetons Plongée et pioche de revues
   sont face cachée, ce qui interdit un Monte-Carlo naïf.

Le risque principal du projet est la **complexité des règles**, pas la
performance de l'IA.

## Pile technique

**Moteur et IA en Java, interface en Angular.**

Retenue parce que le typage fort et JUnit sont précieux face à une surface de
règles aussi large, que la marge de performance pour la recherche Monte-Carlo
est confortable, et que cette pile correspond à la cible déjà mentionnée dans
les conventions de travail — le temps investi resservira ailleurs.

Coût assumé : deux langages, un processus local à lancer, un empaquetage à
prévoir.

### Alternatives écartées

**TypeScript de bout en bout.** Un seul langage, aucun serveur, exécution dans
le navigateur. Écartée pour la marge de performance plus mince sur la recherche
Monte-Carlo, qui aurait contraint le budget d'itérations des niveaux élevés.

**Rust compilé en WebAssembly.** De loin le plus rapide. Écartée pour la courbe
d'apprentissage, et parce que le modèle de propriété de Rust se combine mal avec
un moteur de règles qu'on remanie beaucoup en phase de conception.

## Découpage

| Module | Rôle | Dépend de |
|---|---|---|
| `engine` | Règles pures : état, coups légaux, application, décompte | rien |
| `ai` | Recherche Monte-Carlo | `engine` |
| `app` | Orchestration, API locale, sauvegarde, service de l'interface | `engine`, `ai` |
| `ui` | Interface Angular | l'API de `app` |

Construction : Maven multi-module. `app` sert l'API **et** l'Angular compilé en
fichiers statiques : un seul processus, une seule URL, pas de CORS.

**Règle absolue : `engine` ne connaît ni l'interface, ni le réseau, ni
l'horloge, ni l'IA.** Aucune entrée-sortie, aucun état statique mutable, aucune
source d'aléa implicite. Ce n'est pas une préférence esthétique : l'IA clone
l'état des milliers de fois par seconde, et toute dépendance cachée rend ce
clonage impossible ou faux.

## Décisions structurantes

### 1. La signature centrale

```
legalActions(state) → List<Action>
apply(state, action) → state
score(state) → résultat
```

Une `Action` est une **donnée sérialisable**, pas un appel de méthode : un type
et des paramètres.

Cette signature sert simultanément à quatre usages : l'interface affiche les
coups possibles, l'IA les explore, les tests les rejouent, la sauvegarde les
enregistre. C'est la pièce à ne pas rater.

### 2. Séparer l'état complet de ce qu'un joueur voit

`GameState` contient tout, piles face cachée comprises. `PlayerView` expose ce
qu'un joueur donné peut légitimement observer.

**L'IA ne reçoit jamais qu'une `PlayerView`.** C'est ce qui l'empêche
mécaniquement de tricher, et ce qui la force à déterminiser pour chercher :
tirer un état complet cohérent avec ce qui est observable, chercher dessus,
recommencer.

Cette distinction doit exister dès la première classe écrite. Elle ne se
rétrofit pas.

### 3. État mutable avec copie explicite

À contre-courant de la recommandation habituelle, mais l'immuabilité intégrale
générerait des millions d'objets par recherche.

`GameState` est mutable et porte une méthode `copy()` optimisée, appelée une
fois par simulation. En contrepartie, discipline stricte : **personne en dehors
du moteur ne modifie un état**.

### 4. Les règles spéciales sont des données

Un bus d'événements interne — `TokenClaimed`, `VesselDeparted`, `SonarPlaced`,
`SiteFilled`, `Conserved` — auquel s'abonnent des effets décrits en JSON.

Les 19 règles spéciales de zones du feuillet officiel deviennent 19
descriptions de données, pas 19 conditions éparpillées dans le moteur. Même
mécanisme pour les surcharges de scénario, qui priment sur les règles de base.

Le chargement des données est **strict** : une description invalide empêche le
démarrage, elle ne dégrade pas silencieusement le comportement.

### 5. Position et niveau sont deux nombres distincts

Les quatre pistes d'attribut comptent 13 cases groupées en 5 paliers
(2 + 2 + 3 + 3 + 3). Deux notions s'y superposent, et les confondre serait la
source d'erreurs de décalage la plus probable du projet.

| Notion | Plage | Nature | Rôle |
|---|:-:|---|---|
| `step` | 0 à 12 | position du cube | ce que stocke l'état, ce qui avance d'un cran |
| `level` | 1 à 5 | palier atteint, **déduit** du `step` | ce que lit la logique de jeu |

**Le `step` part de 0** parce que le jeu lui-même compte ainsi : le livret
demande de placer les cubes « sur la case 0 », et la piste de recherche est
imprimée de 0 à 12 sur le plateau. Retenir la base 1 aurait introduit deux
numérotations concurrentes dans le même projet.

**Le `level` part de 1** parce qu'il vaut exactement ce qu'il procure : au
niveau 3, on recrute un spécialiste de rang 3, on prend 3 disques, on récupère
3 disques, on se déplace de 3 cases jusqu'à la profondeur 3. **Le niveau est la
valeur**, sur les quatre pistes. Aucune table de correspondance à écrire.

Les deux bases ne se contredisent pas : un `step` est un décalage depuis le
départ, un `level` est une grandeur. Un décalage se compte à partir de zéro, une
grandeur à partir de un — comme l'indice et la longueur d'une chaîne.

| Niveau | Cases | Valeur | Points | Repère |
|:-:|:-:|:-:|:-:|---|
| 1 | 0 – 1 | 1 | 0 | départ de la partie |
| 2 | 2 – 3 | 2 | 1 | case 2 : submersible bonus (ingéniosité) |
| 3 | 4 – 6 | 3 | 4 | |
| 4 | 7 – 9 | 4 | 7 | case 7 : submersible bonus (ingéniosité) |
| 5 | 10 – 12 | 5 | 10 | case 10 : symbole impact |

Dépassement : au-delà de la case 12, le cube revient à la **case 10** et
rapporte un impact. Les trois repères tombent tous sur la première case d'un
palier.

## Intelligence artificielle

Recherche arborescente Monte-Carlo avec déterminisation, pour traiter
l'information cachée.

L'horizon du jeu est court et fermé — six manches, sept spécialistes, fin
déterministe — ce qui permet de simuler jusqu'au décompte sans heuristique de
coupure.

### Niveaux de difficulté

Un seul algorithme, réglé par le budget de recherche et la qualité des
simulations. Valeurs à calibrer après un premier banc d'essai.

| Niveau | Itérations | Politique de simulation |
|---|---|---|
| Débutant | ~500 | Aléatoire, avec erreurs volontaires |
| Normal | ~3 000 | Aléatoire pondéré |
| Aguerri | ~15 000 | Guidée par heuristiques |
| Confirmé | ~50 000 | Guidée, avec réutilisation de l'arbre entre les tours |

Le facteur limitant sera la **génération des coups légaux**, fonction la plus
appelée du programme. C'est elle qu'il faudra optimiser, avant le langage.

Attention : le scénario *The Spill* comporte des hexagones à points négatifs.
La fonction d'évaluation doit les gérer.

## Sauvegarde

Une partie sauvegardée = **une graine aléatoire + le journal des actions**.

Plus compact qu'un instantané d'état, insensible aux refactorisations du
moteur, et cela offre gratuitement l'annulation d'un coup et le rejeu à
l'identique d'une partie pour déboguer l'IA.

Un instantané périodique pourra s'y ajouter si le rechargement devient lent.

Corollaire : **le moteur doit être déterministe**. Générateur aléatoire à graine
injectée, jamais d'appel implicite au hasard.

## Tests

Les conventions de travail rendent les tests optionnels pour un projet purement
local. On les applique quand même **sur le moteur**, non par principe mais
parce qu'avec 19 règles de zones, 32 revues et 10 scénarios, aucune relecture
humaine ne peut garantir qu'un changement n'a rien cassé ailleurs.

- `engine` : tests unitaires JUnit, et tests de bout en bout rejouant des
  journaux d'actions enregistrés.
- `ai` et `ui` : pas de tests exigés.

## Ordre de travail

1. Squelette Maven multi-module, Spring Boot, Angular : ça démarre et ça affiche
   une page.
2. Chargement des données du matériel, avec validation stricte.
3. Moteur — état, actions, coups légaux, application — **pour la Mission 1
   seulement**.
4. Décompte final.
5. **Partie complète en console avec des joueurs aléatoires.** Jalon de vérité :
   si mille parties s'enchaînent sans exception ni règle violée, le moteur est
   sain.
6. Intelligence artificielle.
7. Interface.

La modélisation du matériel n'a pas besoin d'être complète pour démarrer : la
Mission 1 n'utilise qu'une poignée de tuiles. Relever en priorité les tuiles de
sa mise en place permet de lancer le moteur pendant que le reste du relevé se
poursuit.

## Points encore ouverts

- Format exact de description des effets déclenchés — à concevoir une fois les
  premières tuiles relevées, sur des cas réels plutôt que dans l'abstrait.
- Empaquetage final pour un lancement en un clic.
