# Tasks — Phase 2 : OpenSpec Archive Extraction

**Dépendance :** Phase 1 complétée ✅  
**Règle :** Checklist d'extraction complète AVANT toute suppression.  
**Validation Stevens obligatoire** avant d'appliquer les extractions.

---

## 1. Lire archive 2026-05-05 ✅

- [x] Lire `openspec/changes/archive/2026-05-05-audit-project-docs-and-mkdocs/` entièrement
- [x] `proposal.md` : problème docs éparpillées, objectif inventaire + MkDocs portal
- [x] `design.md` : canonical ownership map, structure MkDocs cible, archive policy (ancienne)
- [x] `tasks.md` : toutes tâches ✅ — script inventory, MkDocs, classification tout livré

## 2. Lire archive 2026-05-07 ✅

- [x] Lire `openspec/changes/archive/2026-05-07-organize-ai-agent-files/` entièrement
- [x] `proposal.md` : problème AI files éparpillés, objectif router + component ownership
- [x] `design.md` : target model router global + component AGENTS.md, context loading rule
- [x] `tasks.md` : toutes tâches ✅ — 6 component AGENTS.md créés, AGENTS.md root allégé

## 3. Matrice d'extraction 2026-05-05 ✅

- [x] Canonical ownership map → couvert par `doc-policy.md` v2.0.0 (Phase 1) — **rien à extraire**
- [x] Structure MkDocs cible → déjà implémentée — **rien à extraire**
- [x] Archive policy "move to docs/archive/" → obsolète, remplacée par `archive-policy.md` v2.0.0 — **rien à extraire**
- [x] Tâches complétées → **rien à extraire**
- **Conclusion : rien de durable → DELETE**

## 4. Matrice d'extraction 2026-05-07 ✅

- [x] Router global + component AGENTS.md → déjà implémenté (6 AGENTS.md existent) — **rien à extraire**
- [x] Context loading rule → dans `00-index.md` (Phase 1) — **rien à extraire**
- [x] Archive policy "move to .ai-archive/" → obsolète, remplacée par `archive-policy.md` v2.0.0 — **rien à extraire**
- [x] Tâches complétées → **rien à extraire**
- **Conclusion : rien de durable → DELETE**

## 5. Validation Stevens ✅

- [x] Matrices présentées sur Slack (`#tchalanet-agents`)
- [x] Approbation reçue : "ok"

## 6–7. Extractions ✅

- [x] Aucune extraction nécessaire — tout déjà dans les docs canoniques

## 8. Supprimer archive 2026-05-05 ✅

- [x] Rien de durable restant — confirmé par matrice
- [x] Stevens a confirmé
- [x] Supprimé `openspec/changes/archive/2026-05-05-audit-project-docs-and-mkdocs/`

## 9. Supprimer archive 2026-05-07 ✅

- [x] Rien de durable restant — confirmé par matrice
- [x] Stevens a confirmé
- [x] Supprimé `openspec/changes/archive/2026-05-07-organize-ai-agent-files/`

## 10. Supprimer dossier archive/ ✅

- [x] Dossier vide après suppression des deux archives
- [x] Supprimé `openspec/changes/archive/`
