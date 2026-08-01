# Tasks

## Convention et composants

- [x] Auditer les écrans détail, liste/tableau et formulaire ciblés et relever les duplications.
- [x] Définir les contrats des primitives partagées : inputs, états, actions, responsive et tokens.
- [x] Ajouter ou compléter le panneau partagé de résumé pour les pages de détail.
- [x] Vérifier les tokens `--tch-*` et `--comp-*` avec les conventions de thème et de style.
- [x] Ajouter `AdminFormLayout` pour standardiser les formulaires avec aperçu ou résumé latéral.

## Migrations

- [x] Ajouter une primitive de refresh partagée et l'utiliser sur les principales listes opérationnelles.

- [x] Migrer le détail seller terminal.
- [x] Migrer le détail ticket.
- [x] Migrer le détail tirage et le détail résultat associé.
- [x] Rendre la liste des terminaux mobile-first avec cartes, identité cliquable et actions visibles.
- [x] Utiliser les métriques partagées pour les statistiques journalières du détail terminal.
- [ ] Migrer la page de commission et ses dialogues d’édition.
- [ ] Migrer les rapports concernés, notamment le rapport des tirages.
- [ ] Remplacer les UUID visibles par des identités métier lisibles, avec fallback technique explicite.
- [ ] Harmoniser les tables : identité, statut, montants, actions, pagination et empty states.
- [ ] Harmoniser les formulaires : labels, aides, erreurs champ/section, pending, succès et annulation.

## Vérification

- [x] Ajouter un contrat e2e vérifiant les identifiants stables des blocs de console opérationnels.
- [x] Ajouter les tests unitaires des primitives et des mappings d’état/statut.
- [ ] Ajouter ou mettre à jour les tests e2e web pour détail, tableau, formulaire, erreur et succès.
- [ ] Vérifier FR/EN/HT, clavier, focus, responsive desktop/mobile et absence de texte codé en dur.
- [ ] Valider les écrans migrés avec `pnpm nx lint`, les tests ciblés et les screenshots e2e.
