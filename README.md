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

Squelette en place. Le moteur de règles n'est pas commencé.

| Étape | État |
|---|---|
| Lecture et synthèse des règles | terminé |
| Architecture du projet | terminé |
| Squelette technique | terminé |
| Modélisation du matériel | en attente des relevés |
| Moteur de règles | à faire |

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

- `specialists.json` — les 21 spécialistes, faces Junior et Senior

Les scans du matériel ne sont pas versionnés, ils appartiennent à l'éditeur.

Les notes de travail sur les règles — synthèse du livret et transcription du
feuillet officiel de clarifications — reprennent le contenu publié par
l'éditeur. Elles restent en local dans `docs/prive/` et ne sont pas versionnées.
Les règles officielles sont disponibles auprès de l'éditeur, voir
[Sources](docs/sources.md).

## Suivi

Les tâches sont suivies sur le tableau **endeavor deep sea** de l'Organiseur.
