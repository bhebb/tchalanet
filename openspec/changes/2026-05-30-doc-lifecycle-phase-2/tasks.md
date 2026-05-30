# Tasks — Phase 2 : OpenSpec Archive Extraction

**Dépendance :** Phase 1 complétée (policies en place).  
**Règle :** Checklist d'extraction complète AVANT toute suppression.  
**Validation Stevens obligatoire** avant d'appliquer les extractions.

---

## 1. Lire archive 2026-05-05

- [ ] Lire `openspec/changes/archive/2026-05-05-audit-project-docs-and-mkdocs/` entièrement
- [ ] Prendre notes : décisions, propositions, canonical ownership map, contexte

## 2. Lire archive 2026-05-07

- [ ] Lire `openspec/changes/archive/2026-05-07-organize-ai-agent-files/` entièrement
- [ ] Prendre notes : structure agents, décisions prises

## 3. Classer et proposer extraction — 2026-05-05

Pour chaque élément identifié :

- [ ] Classifier : norme stable | ADR | context pack IA | rien de durable
- [ ] Proposer destination cible (fichier exact)
- [ ] Produire matrice d'extraction

## 4. Classer et proposer extraction — 2026-05-07

- [ ] Classifier : norme stable | ADR | context pack IA | rien de durable
- [ ] Proposer destination cible
- [ ] Produire matrice d'extraction

## 5. Validation Stevens

- [ ] Présenter les deux matrices (Slack + PR)
- [ ] **Attendre approbation explicite avant toute création/modification**

## 6. Appliquer extractions — 2026-05-05

- [ ] Créer/mettre à jour les fichiers cibles selon matrice validée
- [ ] Vérifier liens dans les nouveaux documents

## 7. Appliquer extractions — 2026-05-07

- [ ] Créer/mettre à jour les fichiers cibles selon matrice validée
- [ ] Vérifier liens

## 8. Supprimer archive 2026-05-05

Checklist avant suppression :
- [ ] Tous les éléments durables extraits ?
- [ ] Tous les liens valides ?
- [ ] Rien de durable restant ?
- [ ] Stevens a confirmé ?

Action :
- [ ] Supprimer `openspec/changes/archive/2026-05-05-audit-project-docs-and-mkdocs/`

## 9. Supprimer archive 2026-05-07

Checklist avant suppression :
- [ ] Éléments durables extraits ou confirmé "rien à extraire" ?
- [ ] Stevens a confirmé ?

Action :
- [ ] Supprimer `openspec/changes/archive/2026-05-07-organize-ai-agent-files/`

## 10. Nettoyer le dossier archive

- [ ] Vérifier que `openspec/changes/archive/` est vide
- [ ] Supprimer `openspec/changes/archive/` si vide
