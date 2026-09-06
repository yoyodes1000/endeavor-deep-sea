# Sources documentaires

## Documents en notre possession

Ces documents appartiennent à l'éditeur et ne sont pas versionnés. Ils sont
conservés en local, hors du dépôt.

| Document | Fichier | Langue | Contenu |
|---|---|---|---|
| Livret de règles | `FR_ENDS_rules_versionok_comp.pdf` | FR | 16 pages, règles complètes du mode compétitif + section Co-Op/Solo |
| Feuillet de clarifications | `ENDS_clarifications-sheet_web.pdf` | EN | 2 pages, règles spéciales des 18 zones + clarifications des 10 scénarios |

Le livret français est le PDF officiel publié par Super Meeple. Le feuillet de
clarifications a été récupéré sur BoardGameGeek ; **il n'existe pas de version
française en ligne**, le feuillet FR n'est distribué que dans la boîte.

### Extraction

Les deux PDF sont des fichiers d'impression : le texte y est présent mais les
schémas et icônes portent une part essentielle de l'information. L'appariement
zone / règle du feuillet, notamment, n'est lisible que sur le rendu visuel.

Outil utilisé : **PyMuPDF** (`pip install pymupdf`), qui fournit à la fois
l'extraction de texte et le rendu des pages en image.

## Liens

- [BGG — Endeavor: Deep Sea](https://boardgamegeek.com/boardgame/367966/endeavor-deep-sea)
- [BGG — English Clarifications Sheet](https://boardgamegeek.com/filepage/286338/endeavor-deep-sea-english-clarifications-sheet) *(téléchargement soumis à connexion)*
- [BGG — English rule book](https://boardgamegeek.com/filepage/283177/endeavor-deep-sea-english-rule-book)
- [BGG — discussion sur les règles spéciales des zones](https://boardgamegeek.com/thread/3088793/clarification-some-special-rules-ocean-boards)
- [Burnt Island Games — page officielle](https://www.burntislandgames.com/endeavordeepsea)
- [Super Meeple — fiche du jeu](https://www.supermeeple.com/nos-jeux/endeavorepp/)

## Données encore manquantes

Ces données ne figurent dans aucun document publié et devront être relevées à
partir du matériel physique.

| Donnée | Volume | Carte de l'Organiseur |
|---|---|---|
| Tuiles Océan : pistes sonar, sites, bonus, connexions | 37 tuiles | 04 |
| Spécialistes, recto Junior et verso Senior | 21 modèles, 51 tuiles | 05 |
| Fiches Scénario : mise en place, objectifs | 10 scénarios | 06 |
| Revues scientifiques | 24 + 8 de départ | 07 |
| Jetons Plongée | 36 jetons, dont 4 seulement illustrés dans le livret | 09 |

Les **plateaux Impact des 10 scénarios sont relevés** dans `data/missions.json`
(orientation, hexagones, gains, symboles de domaine, points, capacité, îlots) ;
restent la mise en place et les objectifs de chaque fiche. Le relevé a été fait
sur les scans des fiches, superposition de contrôle à l'appui, et les barèmes
couleur→points confrontés au matériel physique.

**Piste de recoupement** : un [mod Tabletop Simulator](https://steamcommunity.com/sharedfiles/filedetails/?id=2976002512)
du jeu existe et contient nécessairement les visuels de tout le matériel. Utile
si les scans laissent des zones d'ombre.

## Fiabilité

- La synthèse des règles a été établie sur le texte français intégral, recoupé
  avec le rendu visuel des pages pour les tableaux et les icônes.
- La structure des pistes d'attribut (13 cases, paliers 2/2/3/3/3, décompte
  0/1/4/7/10) a été vérifiée sur le rendu du plateau joueur.
- L'appariement zone / règle spéciale a été établi sur le rendu visuel du
  feuillet, l'ordre du texte extrait n'étant pas fiable.
- Les noms de zones et de missions sont donnés en anglais faute de source
  française ; ils devront être confrontés au matériel français.
