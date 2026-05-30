# Proposal — Phase 1 : Policy + Inventory

**Date :** 2026-05-30  
**Type :** Gouvernance documentaire (cross-project)  
**Branch :** `docs/doc-lifecycle-spec`  
**Contexte global :** `openspec/changes/2026-05-30-doc-lifecycle-spec/proposal.md`  
**Statut :** À faire

---

## Why

Les règles documentaires ne sont pas codifiées dans les fichiers de policy existants. `openspec/context/00-index.md` référence des packs qui n'existent pas. `DOCUMENTATION.md` pointe vers `.claude/skills/` supprimé. Sans inventaire frais, on ne sait pas quoi corriger en Phase 2 et 3.

---

## What

Poser les règles, corriger les références cassées, et cartographier la dette documentaire. Phase de lecture et d'écriture légère — aucune modification de contenus source.

- Mettre à jour `doc-policy.md` : hiérarchie de vérité, ownership, lifecycle OpenSpec, PR checklist
- Réécrire `archive-policy.md` : interdire `archive/`, checklist d'extraction
- Nettoyer `openspec/context/00-index.md` : retirer les packs fantômes
- Corriger les liens `.claude/skills/` → `.agents/skills/`
- Produire inventaires : openspec/changes, tchalanet-docs/docs, conventions server, flows manquants

---

## Impact

- `tchalanet-docs/docs/00-guidelines/doc-policy.md` — mis à jour
- `tchalanet-docs/docs/06-openspec/archive-policy.md` — réécrit
- `openspec/context/00-index.md` — nettoyé
- `DOCUMENTATION.md` (root) — lien corrigé
- Tableaux d'inventaire produits (pas de suppression dans cette phase)

---

## Non-goals

- Ne pas supprimer les archives (Phase 2)
- Ne pas modifier les contenus near-code (conventions, DOMAIN_*.md)
- Ne pas créer des context packs manquants
- Ne pas toucher : `ai-agents/`, `setup-ai-agents-slice-first/`
