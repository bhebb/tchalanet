# Proposal — Lifecycle documentaire Tchalanet

**Date :** 2026-05-30  
**Type :** Gouvernance documentaire (cross-project)  
**Branch :** `docs/doc-lifecycle-spec`  
**Statut :** Draft

---

## Why

646 fichiers Markdown dans le repo. Les règles sont éparpillées, dupliquées, introuvables. Un document inaccessible ne sert à rien.

Problèmes concrets :
- `openspec/context/00-index.md` référence ~10 packs, 8 n'existent pas → les agents hallucinent
- Les archives OpenSpec accumulent des décisions qui auraient dû devenir des normes
- Pas de règle claire sur ce qui sort de `openspec/changes/` quand une feature est livrée
- `DOCUMENTATION.md` à la racine pointe vers `.claude/skills/` qui n'existe plus
- `tchalanet-server` et `tchalanet-infra` ont de la dette ; web/mobile/edge/docs risquent de la reproduire
- Pas de `pnpm docs:check` — aucune validation automatique de cohérence documentaire

---

## What

Établir une gouvernance documentaire claire en 3 phases, de sorte qu'un agent ou un développeur sache exactement où se trouve chaque type d'information, comment le maintenir, et quand le supprimer.

**Phase 1 — Policy + Inventory** *(tâches actives → `tasks.md`)*  
Poser les règles, corriger les refs cassées, cartographier la dette. Risque bas.

**Phase 2 — OpenSpec archive extraction** *(tâches futures → `design.md`)*  
Traiter les 2 archives, extraire les éléments durables, supprimer les archives. Risque moyen.

**Phase 3 — Full audits** *(tâches futures → `design.md`)*  
Audits server/infra (corriger la dette), web/mobile/edge/docs (prévention). Risque haut.

Détails complets : `design.md`.

---

## Impact

- `tchalanet-docs/docs/00-guidelines/doc-policy.md` — mis à jour (hiérarchie, ownership, PR checklist)
- `tchalanet-docs/docs/06-openspec/archive-policy.md` — réécrit (interdire archive/, checklist d'extraction)
- `openspec/context/00-index.md` — nettoyé (retirer packs fantômes)
- `DOCUMENTATION.md` (root) — lien corrigé
- `openspec/changes/archive/2026-05-05-*` et `archive/2026-05-07-*` — extraits puis supprimés (Phase 2)
- `tchalanet-server/docs/conventions/` — gaps identifiés (Phase 3)
- Tous composants — structure documentaire minimale établie (Phase 3)

---

## Non-goals

- Ne pas réécrire le contenu des conventions existantes (vérifier cohérence uniquement)
- Ne pas créer les context packs manquants (identifier les gaps seulement)
- Ne pas toucher les changes actives : `ai-agents/`, `setup-ai-agents-slice-first/`
- Ne pas créer `pnpm docs:check` dans cette change (identifier le besoin, pas l'implémenter)
- Ne pas modifier les sources near-code sans validation humaine sur chaque fichier
