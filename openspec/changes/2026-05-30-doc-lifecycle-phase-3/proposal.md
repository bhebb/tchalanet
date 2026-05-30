# Proposal — Phase 3 : Full Audits + Prévention

**Date :** 2026-05-30  
**Type :** Gouvernance documentaire (cross-project)  
**Branch :** à créer après Phase 2  
**Contexte global :** `openspec/changes/2026-05-30-doc-lifecycle-spec/proposal.md`  
**Statut :** Placeholder (affiner après Phase 1 & 2)

---

## Why

`tchalanet-server` et `tchalanet-infra` ont accumulé de la dette documentaire : conventions manquantes, ARCHITECTURE.md possiblement obsolète, DOMAIN_*.md inexistants dans certaines slices. `tchalanet-web`, `tchalanet-mobile`, `tchalanet-edge-service` et `tchalanet-docs` n'ont pas encore de formalisme établi — sans intervention préventive, ils reproduiront la même dette dans 6-12 mois.

---

## What

Auditer tous les composants et établir le formalisme minimal avant que la dette s'accumule.

**Priorité haute (dette existante) :**
- `tchalanet-server` : vérifier ARCHITECTURE.md, inventorier conventions, vérifier DOMAIN_*.md
- `tchalanet-infra` : analyser docs, identifier conventions manquantes, produire liste de nettoyage

**Priorité moyenne (prévention) :**
- `tchalanet-web` : établir structure minimale (ARCHITECTURE.md, conventions Angular/Nx/NgRx)
- `tchalanet-mobile` : établir structure minimale (Flutter, secure storage, offline sync)
- `tchalanet-edge-service` : établir structure minimale
- `tchalanet-docs` : résoudre orphelins, nettoyer 99-reference/, identifier flows manquants

Processus par composant : audit → matrice → validation Stevens → exécution.

---

## Impact

- `tchalanet-server/docs/` — conventions complétées, ARCHITECTURE.md à jour
- `tchalanet-infra/docs/` — docs obsolètes supprimés, conventions créées
- `tchalanet-web/docs/` — structure minimale établie
- `tchalanet-mobile/docs/` — structure minimale établie
- `tchalanet-edge-service/docs/` — structure minimale établie
- `tchalanet-docs/docs/` — orphelins résolus, 99-reference/ nettoyé

---

## Non-goals

- Ne pas réécrire entièrement les conventions existantes (corriger les gaps uniquement)
- Ne pas créer des docs pour des règles qui n'existent pas encore dans le code
- Ne pas supprimer sans validation humaine explicite
- Ne pas démarrer avant Phase 2 complétée
