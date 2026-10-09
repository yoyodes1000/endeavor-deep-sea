# Endeavor : Eaux profondes — adaptation numérique

Implémentation jouable en solo du jeu de société *Endeavor: Deep Sea*
(Carl de Visser & Jarratt Gray, Burnt Island Games / Grand Gamers Guild, 2024),
avec des adversaires pilotés par une intelligence artificielle de type
Monte-Carlo.

Projet personnel, à usage privé.

## Objectif

Jouer une partie en **mode compétitif** contre 1 à 3 adversaires artificiels,
dont le niveau de difficulté est choisi individuellement en début de partie.

À noter : le mode solo officiel du jeu est un mode **coopératif à un joueur**,
sans adversaire. Ce que nous construisons — du compétitif contre des bots —
n'est pas une variante prévue par l'éditeur.

## État

Le moteur fait jouer une partie complète, des deux phases de chaque manche
jusqu'au décompte final. Une API locale permet de la jouer contre des bots qui
choisissent leurs coups au hasard. L'interface de jeu reste à faire.

| Étape | État |
|---|---|
| Lecture et synthèse des règles | terminé |
| Architecture du projet | terminé |
| Squelette technique | terminé |
| Modélisation du matériel | terminé |
| Moteur de règles | jouable sur les missions 1, 2, 4, 7, 8, 9 et 10 ; restent les règles spéciales des zones, les actions des revues acquises et les connexions entre sites |
| API locale | partie contre des bots aléatoires |
| Intelligence artificielle | à faire (bots aléatoires seulement) |
| Interface de jeu | à faire |

## Construire et lancer

**Prérequis** : JDK 21 ou plus, Maven 3.9, Node 20 ou plus.

Construire l'interface, puis le tout :

```
cd ui && npm install && npm run build
cd .. && mvn clean install
```

`npm run build` dépose l'interface compilée dans les ressources de Spring Boot ;
l'application est donc autonome.

Lancer :

```
java -jar app/target/app-0.1.0-SNAPSHOT.jar
```

Puis ouvrir <http://localhost:8080>.

### En développement

Deux processus, pour bénéficier du rechargement à chaud de l'interface :

```
mvn -pl app spring-boot:run
cd ui && npm start
```

L'interface est alors sur <http://localhost:4200> et relaie les appels `/api`
vers le port 8080, ce qui évite toute configuration CORS.

### Tests

```
mvn test              # moteur
cd ui && npm test     # interface
```

## API locale

L'application expose une API sur `http://127.0.0.1:8080/api`. Le joueur humain
occupe le siège 0, les bots les autres.

| Route | Rôle |
|---|---|
| `GET /api/missions` | Les missions jouables, en indiquant si tous leurs objectifs sont comptés |
| `POST /api/game` | Nouvelle partie : `{"mission": 1, "opponents": 2}`, de 1 à 3 adversaires. Une graine `seed` facultative rejoue une partie à l'identique |
| `GET /api/game` | La partie vue par le joueur : phase, manche, joueur courant, coups légaux, coups joués par les bots |
| `POST /api/game/moves` | Jouer un coup : `{"moveNumber": 3, "action": {"type": "Recruter", "specialistId": "pilot"}}` |
| `GET /api/game/result` | Le décompte final, une fois la partie terminée |

Un coup est un objet JSON qui porte son `type`, le nom du coup dans le moteur,
et ses paramètres. Le serveur n'accepte qu'un coup présent dans la liste des
coups légaux. `moveNumber` reprend celui de la dernière vue lue : un coup choisi
sur un état périmé est refusé. Les bots jouent dans la même requête, jusqu'à ce
que la main revienne au joueur.

Les erreurs suivent le format `application/problem+json` : 400 pour une requête
invalide, 404 sans partie en cours, 409 pour un coup périmé ou un décompte
demandé trop tôt, 415 pour un corps qui n'est pas du JSON, 422 pour un coup
illégal.

L'application n'écoute que sur la boucle locale. Elle refuse toute requête
adressée à un autre nom d'hôte que `localhost` ou `127.0.0.1`, ce qui la protège
du *DNS rebinding*.

## Documentation

- [Architecture](docs/architecture.md) — décisions structurantes, alternatives
  écartées, ordre de travail
- [Glossaire anglais → français](docs/glossaire-en-fr.md) — correspondance des
  termes et identifiants de code
- [Sources](docs/sources.md) — documents de référence, méthode d'extraction,
  données manquantes

## Données du matériel

`data/` contient la description du matériel de jeu, relevée sur la boîte et
chargée au démarrage. Ces fichiers ne dépendent d'aucun module : le moteur les
lit, l'application les sert.

- `specialists.json` — les spécialistes, faces Junior et Senior, chefs
  d'équipe compris
- `missions.json` — les 10 scénarios : plateau Impact, mise en place de l'océan,
  zone de lancement et objectifs
- `ocean-tiles.json` — les 37 tuiles Océan et leurs sites
- `dive-tokens.json` — les jetons Plongée, par type et nombre d'exemplaires
- `journals.json` — les 32 revues
- `player-board.json` — le plateau joueur : pistes et zones

Au build, ces fichiers sont copiés dans le jar de l'application, qui les charge
une fois au démarrage. Une description invalide empêche le démarrage.

Le champ `actions` se lit à deux niveaux : la liste **externe** énumère les
actions cumulées, la liste **interne** les alternatives. `[["travel", "sonar"]]`
donne une action au choix entre Voyage et Sonar — le trait oblique imprimé sur
la carte — tandis que `[["sonar"], ["dive"]]` donne les deux, l'une puis l'autre.

Les scans du matériel ne sont pas versionnés, ils appartiennent à l'éditeur.

Les notes de travail sur les règles — synthèse du livret et transcription du
feuillet officiel de clarifications — reprennent le contenu publié par
l'éditeur. Elles restent en local dans `docs/prive/` et ne sont pas versionnées.
Les règles officielles sont disponibles auprès de l'éditeur, voir
[Sources](docs/sources.md).

## Suivi

Les tâches sont suivies sur le tableau **endeavor deep sea** de l'Organiseur.
