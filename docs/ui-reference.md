# Interface — écran de partie (référence carte 10)

Référence validée le 2026-09-11 pour la carte 10 (« mise en place de l'UI »).
Ce document fige les partis-pris de l'écran de jeu ; il est la cible de
l'implémentation Angular à venir. Il complète la décision 7 d'`architecture.md`
(et sa révision).

**Maquette interactive** (design canvas, privée — accessible au propriétaire du
dépôt) : <https://claude.ai/code/artifact/a13f4010-c53b-452a-a93b-fddabdfd2f44>.
Deux pages : *Écran de partie* et *Flux Sonar*. Le présent document se suffit à
lui-même si le lien n'est pas ouvrable.

## Structure de l'écran (ordre vertical)

Bandeau → **Spécialistes des adversaires** (visibles) → **Cartes publication** →
**Océan** → **Plateau du joueur**.

- **Bandeau** : titre + couleur du joueur et des adversaires.
- **Spécialistes adverses** : toujours visibles, un bloc par joueur sur fond
  **pastel** de sa couleur, avec l'état des actions (libre / utilisée) — pour voir
  d'un coup d'œil ce qu'il reste aux adversaires.
- **Cartes publication** : pioche + rangée disponible.
- **Océan** : grille 5 colonnes (A–E) × 5 profondeurs ; zone centrale.
- **Plateau du joueur** : 4 pistes d'attribut (0→12, impact case 10, submersible
  bonus ingéniosité 2 & 7) + piste recherche + zones + mes spécialistes, avec un
  **rappel des points** au-dessus.

## Partis-pris

- **Habillage par scans** pour les **tuiles Océan** et les **spécialistes**
  (cf. décision 7, révision) : le scan est l'image, la **logique et les zones
  cliquables restent pilotées par le JSON** (positions : cf. section
  *Zones cliquables — convention de coordonnées*). Scans **en local
  uniquement** ; **placeholders** dans toute maquette publiée.
- **Tableau de mission** : dessiné en **DOM** depuis `missions.json` (décision 7
  conservée : adjacence calculée, clone propre sans scans).
- **Panneaux masqués = tiroirs** qui sortent du bord de l'écran via une **flèche** :
  *pistes adverses* (bord gauche), *tableau de mission* et *tuiles spécialistes
  disponibles* (bord droit).
- **Actions déclenchées par clic sur un site** d'une tuile : **S** sonar,
  **D** plongée, **C** conservation, **P** publication.
- **Submersibles** : jeton(s) posé(s) dans une **zone dédiée en haut de la tuile**
  (action Voyage ; une tuile peut en porter plusieurs). La **zone de transit**
  affiche le **stock de submersibles restants**.
- **Jeton Plongée** : case **ronde**, un seul stockable.
- **3 adversaires** : la mise en page est dimensionnée pour trois.
- **Palette** : turquoise des mers du sud `#12a4b6` + jaune dominant de la boîte
  `#F0C43A` (« Yellow Submarine »).

## Zones cliquables sur scans — convention de coordonnées

Les hotspots (sites cliquables posés par-dessus un scan) sont un **overlay
séparé** : `data/hotspots.json`, distinct des données de jeu. `ocean-tiles.json`
(et les autres) restent de la **logique pure** ; la géométrie de présentation vit
à part (responsabilité unique). L'overlay mappe `tuile → site → position`.

- **Unité : relative, jamais en pixels.** Chaque valeur est une **fraction
  ∈ [0, 1]** des dimensions intrinsèques du scan. Conséquence : les positions
  résistent au **redimensionnement d'affichage** et au **changement de machine**
  (le relevé suit le projet, pas l'ordinateur). Seul un **re-scan** au cadrage
  différent impose un nouveau relevé.
- **Forme d'un hotspot : centre + rayon.** `{ "x", "y", "r" }` où `x`,`y` sont le
  **centre** (fractions de la largeur / hauteur) et `r` le **rayon** (fraction de
  la largeur). Colle aux icônes rondes du jeu et se rend en **bouton focalisable
  et étiqueté** (cf. a11y ci-dessous). Un besoin rectangulaire ultérieur ajoutera
  `w`/`h` — pas avant (YAGNI).
- **Clés = id de sites existants.** L'overlay réutilise les identifiants déjà
  définis dans les données (`d1` plongée, `s1`/`s2` sonar, `c1` conservation…) ;
  il ne fait que les **positionner**, il n'invente aucune logique.

```json
{
  "atoll": {
    "d1": { "x": 0.50, "y": 0.62, "r": 0.07 },
    "s1": { "x": 0.28, "y": 0.30, "r": 0.05 },
    "s2": { "x": 0.42, "y": 0.30, "r": 0.05 }
  }
}
```

Le fichier `data/hotspots.json` **n'est pas encore créé** : il le sera au relevé
proprement dit, tuile par tuile, une fois les scans sous la main.

## Accessibilité (WCAG 2.2 AA)

- Tiroirs : boutons avec `aria-expanded` / `aria-controls`, focus géré, panneaux
  atteignables au clavier.
- Identité des adversaires : jamais par la couleur seule (puce **+** libellé).
- Zones cliquables sur scans : **boutons focalisables et étiquetés**, pas une
  simple image-map ; toute pose au **glisser-déposer** aura une alternative
  clavier/bouton.

## Flux d'action — exemple Sonar

Clic sur un site **S** → fenêtre **« choisir la profondeur »** → propose les
**2 premières tuiles** de la pile de cette profondeur → choix → retour à l'océan
pour désigner la **case**. Schéma réutilisable pour Plongée / Publication.

## Points encore ouverts

Journal des coups des bots ; bouton Annuler (l'architecture le permet : graine +
journal) ; pion Premier joueur ; usage des cartes Revers / Objectif bonus en mode
compétitif.

## À réconcilier

Identifiants des zones du plateau joueur : `data/player-board.json` nomme
`transit` / `actionDiscReserve` / `diveTokenStorage`, tandis que
`docs/glossaire-en-fr.md` fixe `stagingArea` / `supply` / `diveTokenSlot`. Aligner
sur un seul jeu de noms.
