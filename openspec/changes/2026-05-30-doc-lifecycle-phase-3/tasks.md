# Tasks — Phase 3 : Full Audits + Prévention

**Dépendance :** Phase 1 + Phase 2 complétées.  
**Règle :** Validation humaine avant toute création, modification ou suppression.  
**Principe :** Créer uniquement ce qui manque — ne pas surcharger.

---

## tchalanet-server

### 1. Audit server

- [ ] Lire `tchalanet-server/docs/ARCHITECTURE.md` — référence tous les composants ? à jour ?
- [ ] Lister fichiers dans `tchalanet-server/docs/conventions/`
- [ ] Identifier conventions manquantes (batch, cache, persistence, RLS, etc.)
- [ ] Lister `src/**/DOMAIN_*.md` par slice — existence + cohérence surface
- [ ] Produire matrice : convention/DOMAIN | existe | à jour | action (créer | mettre à jour | supprimer)

### 2. Valider + exécuter server

- [ ] Proposer matrice à Stevens, attendre approbation
- [ ] Mettre à jour `ARCHITECTURE.md` si nécessaire
- [ ] Créer/mettre à jour conventions manquantes (max 5-7 fichiers)
- [ ] Créer/vérifier `DOMAIN_*.md` manquants par slice

---

## tchalanet-infra

### 3. Audit infra

- [ ] Lister tous les docs dans `tchalanet-infra/docs/`
- [ ] Évaluer chacun : utile pour créer/modifier un module infra ? (oui/non/obsolète)
- [ ] Identifier conventions manquantes (déploiement, secrets, réseau, volumes)
- [ ] Produire liste : garder | mettre à jour | supprimer | créer

### 4. Valider + exécuter infra

- [ ] Proposer liste à Stevens, attendre approbation
- [ ] Supprimer docs obsolètes
- [ ] Mettre à jour docs existants si nécessaire
- [ ] Créer conventions manquantes

---

## tchalanet-web

### 5. Audit web

- [ ] Lister docs existants (README, docs/, conventions si présentes)
- [ ] Vérifier cohérence surface avec le code actuel
- [ ] Identifier conventions manquantes (Angular/Nx, NgRx, theming, page model, i18n)
- [ ] Produire liste : à jour | à mettre à jour | à créer

### 6. Valider + exécuter web

- [ ] Proposer liste à Stevens, attendre approbation
- [ ] Créer `docs/ARCHITECTURE.md` si absent
- [ ] Créer `docs/conventions/` si absent
- [ ] Ajouter conventions clés (2-3 fichiers max)

---

## tchalanet-mobile

### 7. Audit mobile

- [ ] Lister docs existants
- [ ] Vérifier cohérence surface avec le code
- [ ] Identifier conventions manquantes (Flutter, secure storage, offline sync, terminal binding)
- [ ] Produire liste

### 8. Valider + exécuter mobile

- [ ] Proposer liste à Stevens, attendre approbation
- [ ] Créer structure minimale + conventions clés

---

## tchalanet-edge-service

### 9. Audit edge

- [ ] Lister docs existants
- [ ] Vérifier cohérence surface
- [ ] Identifier conventions manquantes
- [ ] Produire liste

### 10. Valider + exécuter edge

- [ ] Proposer liste à Stevens, attendre approbation
- [ ] Créer structure minimale + conventions clés

---

## tchalanet-docs

### 11. Audit + cleanup tchalanet-docs

- [ ] Lire dossiers orphelins (`02-domains/`, `03-apps/`, `05-decisions/`)
  - Contenu unique ou doublons dans les dossiers canoniques ?
  - Proposer : fusionner | rediriger | supprimer
- [ ] Analyser `99-reference/` (rapports générés) — exclure nav MkDocs ou supprimer
- [ ] Lister flows dans `02-functional/flows/` — workflows métier clés manquants ?

### 12. Valider + exécuter tchalanet-docs

- [ ] Proposer plan complet à Stevens, attendre approbation
- [ ] Fusionner/rediriger/supprimer orphelins selon plan validé
- [ ] Nettoyer `99-reference/`
- [ ] Créer liste de flows manquants (pas les flows eux-mêmes)

---

## Bilan Phase 3

- [ ] Tous les composants ont `docs/ARCHITECTURE.md` + `docs/conventions/` minimal
- [ ] Aucun dossier orphelin dans `tchalanet-docs/`
- [ ] `openspec/changes/archive/` inexistant
- [ ] Tous les liens valides (`pnpm docs:check` passe)
