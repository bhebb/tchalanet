# Web console surface consistency v1

## Why

Les écrans privés utilisent déjà les mêmes briques de console, mais les pages de détail, les
tableaux et les formulaires divergent encore sur la structure, la hiérarchie visuelle, les actions,
les états asynchrones et les textes. Les panneaux latéraux des terminaux, tickets et tirages sont
notamment construits avec trois chartes différentes.

Cette divergence rend les écrans difficiles à parcourir et augmente le coût de maintenance : chaque
feature recrée ses cartes, ses erreurs, ses actions et ses règles responsive.

## What

Définir et appliquer une charte commune pour les trois archétypes de la console web :

- **détail** : page shell, identité, statut, actions, sections de faits et résumé latéral ;
- **liste/tableau** : filtres, table, identité lisible, actions de ligne, pagination et états vides ;
- **formulaire** : structure, validation, erreurs de champ, résumé d’erreur, mutation et succès.

Les primitives de structure et de design vivent dans `@tch/ui/console`. Les composants de domaine
présentant des données communes aux consoles vivent dans `@tch/web/console` et consomment ces
primitives. Les pages restent propriétaires de leurs données, de leurs routes et de leurs règles
métier.

La première migration cible les détails des seller terminals, tickets et tirages, la page de
commission et les rapports qui présentent actuellement des identifiants techniques ou des panneaux
incohérents.

## Impact

- Ajout ou évolution de primitives dans `libs/ui/console`.
- Réduction des styles et du markup spécifiques aux pages.
- Harmonisation des états loading, error, empty, success et retry.
- Harmonisation i18n FR/EN/HT et accessibilité clavier/lecteur d’écran.
- Aucun changement de règle métier ou de contrat backend imposé par cette convention.

## Non-goals

- Refaire la navigation ou le shell global.
- Transformer les pages PageModel publiques.
- Modifier la logique de commission, de vente ou de reporting dans cette proposition.
- Imposer un contenu identique à toutes les pages : seules la structure et la charte sont communes.
