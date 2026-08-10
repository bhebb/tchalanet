# Tasks — Guides visuels admin orientés actions

---

## T0 — OpenSpec skeleton

- [x] Créer `proposal.md`
- [x] Créer `tasks.md`

## T1 — Parcours prioritaires à clarifier

- [x] Ajouter une capture dédiée `Bloke yon nimewo`
- [x] Ajouter une capture du formulaire de blocage avec numéro et bouton `Anrejistre`
- [x] Annoter le parcours `bloquer un numéro` dans le PDF opérations
- [ ] Ajouter une capture du menu actions machann ouvert
- [ ] Ajouter une capture de confirmation `bloquer machann`
- [ ] Ajouter une capture de confirmation / résultat `réinitialiser PIN`
- [ ] Ajouter une capture après création machann réussie
- [ ] Ajouter une capture de vente refusée à cause d'un numéro bloqué

## T2 — Structure des parcours

- [ ] Ajouter pour chaque parcours un bloc `Objectif`
- [ ] Ajouter pour chaque parcours un bloc `Résultat attendu`
- [ ] Ajouter pour chaque parcours un bloc `Si ça ne marche pas`
- [ ] Remplacer les notes floues par des captures annotées quand l'action existe dans l'app

## T3 — Cohérence wording

- [ ] Vérifier que les actions visibles reprennent les libellés réels de l'app
- [ ] Garder `machann` pour l'utilisateur vendeur/admin
- [ ] Garder `machin POS` seulement quand on parle de l'appareil ou de l'accès technique
- [ ] Éviter `tenant` dans les textes client-facing

## T4 — Génération et QA visuelle

- [ ] Régénérer les captures avec `TCH_CAPTURE_GUIDES=1`
- [ ] Régénérer les deux PDF admin
- [ ] Rendre les PDF en PNG avec Poppler
- [ ] Inspecter les pages critiques : création machann, blocage machann, reset PIN, blocage numéro, rapports
- [ ] Vérifier que les annotations ne masquent pas les boutons/champs

## T5 — Intégration docs

- [ ] Intégrer les parcours renforcés dans les pages d'aide admin MkDocs
- [ ] Préparer la même logique pour le futur guide vendeur
- [ ] Mettre à jour la PR avec la liste des parcours couverts et ceux restant à capturer
