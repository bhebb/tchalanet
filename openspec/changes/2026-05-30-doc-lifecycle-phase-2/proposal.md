# Proposal — Phase 2 : OpenSpec Archive Extraction

**Date :** 2026-05-30  
**Type :** Gouvernance documentaire (cross-project)  
**Branch :** à créer après Phase 1  
**Contexte global :** `openspec/changes/2026-05-30-doc-lifecycle-spec/proposal.md`  
**Statut :** En attente (démarrer après Phase 1)

---

## Why

Deux archives OpenSpec accumulent des décisions qui auraient dû devenir des normes. Elles occupent de l'espace, créent de la confusion, et leur présence viole la règle établie en Phase 1 (pas d'archive, extraction obligatoire avant suppression).

Archives concernées :
- `openspec/changes/archive/2026-05-05-audit-project-docs-and-mkdocs/`
- `openspec/changes/archive/2026-05-07-organize-ai-agent-files/`

---

## What

Pour chaque archive :
1. Lire entièrement et classer chaque élément (norme stable | ADR | context pack IA | rien de durable)
2. Produire matrice d'extraction avec destination exacte
3. Validation Stevens
4. Appliquer les extractions
5. Supprimer l'archive après checklist

---

## Impact

- Éléments durables extraits vers `tchalanet-docs/docs/00-guidelines/`, `03-adr/`, ou `openspec/context/`
- `openspec/changes/archive/2026-05-05-*` — supprimé
- `openspec/changes/archive/2026-05-07-*` — supprimé
- `openspec/changes/archive/` — supprimé si vide
- Maximum 5-7 nouveaux documents créés ou mis à jour

---

## Non-goals

- Ne pas toucher server, infra, web, mobile, edge, docs (Phase 3)
- Ne pas modifier les documents existants sauf pour y ajouter les extractions
- Ne pas supprimer une archive sans avoir produit et appliqué la matrice d'extraction
