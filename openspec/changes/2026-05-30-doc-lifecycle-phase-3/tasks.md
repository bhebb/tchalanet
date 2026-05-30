# Tasks — Phase 3 : Full Audits + Prévention

**Dépendance :** Phase 1 + Phase 2 complétées ✅  
**PR :** #112 mergée le 2026-05-30

---

## Archives component ✅

- [x] Lire et classer 33 archives server + 6 archives infra/mobile/edge
- [x] Matrice d'extraction : 28 changes DELETE, 5 extractions (context docs, DOMAIN_ANALYTICS, common README)
- [x] Validation Stevens
- [x] Créer `DOMAIN_ANALYTICS.md` near-code dans `core/analytics/`
- [x] Mettre à jour `tchalanet-common/README.md` en fonction du code réel
- [x] Supprimer 250 fichiers d'archives (server + infra + mobile + edge)

## Documentation contexte de connexion ✅

- [x] Créer `tchalanet-server/docs/conventions/context/README.md`
- [x] Créer `request-context.md` — contexte universel, pipeline HTTP, resolvers, tenant policy, batch, RLS
- [x] Créer `operational-context.md` — contexte POS/terrain, sources, fail-fast, owner boundaries
- [x] Créer `role-flows.md` — flows par rôle (seller, admin POS, super-admin, system, public)
- [x] Créer `tchalanet-docs/docs/02-functional/flows/role-login-flow.visual.html`
- [x] Supprimer fichiers pointeurs éparpillés (`user-contexte-operational.md`, `request_context_usage.md`)
- [x] `docs/architecture/OPERATIONAL_CONTEXT.md` → pointeur vers `conventions/context/`

## tchalanet-docs — orphelins + 99-reference ✅

- [x] Supprimer `00-audit/` (audits ponctuels avril 2026)
- [x] Supprimer `02-domains/` (index vide)
- [x] Supprimer `03-apps/` (doublon de `99-links/`)
- [x] `OFFLINE_SALES_RISK_POLICY.md` déplacé → `03-adr/`
- [x] Supprimer `05-decisions/`
- [x] `99-reference/` : planning one-shot supprimé, générés exclus de la nav MkDocs
- [x] `mkdocs.yml` nettoyé

## tchalanet-web ✅

- [x] Créer `docs/ARCHITECTURE.md` — structure `tch-portal`, libs cible, convention Page/Container/Component
- [x] Créer `docs/conventions/` avec 5 conventions migrées depuis `docs/web/`
  (`naming`, `state-management`, `nx-boundaries`, `feature-playbook`, `placement-guide`)

## tchalanet-mobile ✅

- [x] Créer `docs/conventions/README.md` — index des conventions existantes

## tchalanet-edge-service ✅

- [x] Créer `docs/ARCHITECTURE.md` — stack Fastify/TS, rôle, endpoints, providers
- [x] Créer `docs/conventions/README.md`

## Bilan Phase 3 ✅

- [x] Tous les composants ont `docs/ARCHITECTURE.md` ou équivalent + `docs/conventions/`
- [x] Aucun dossier orphelin dans `tchalanet-docs/`
- [x] `openspec/changes/archive/` inexistant dans tous les composants
- [x] Flows manquants identifiés (TODO dans `role-flows.md`)
- [ ] `pnpm docs:check` — à implémenter (Phase future)
- [ ] `DOMAIN_*.md` near-code server — dette identifiée, à traiter séparément

## Dette restante documentée

- `DOMAIN_*.md` : 0 fichiers near-code dans `tchalanet-server/src/` — gap connu, à traiter slice par slice
- `pnpm docs:check` : tool non implémenté — identifié dans `doc-policy.md`
- Flows fonctionnels manquants : seller-onboarding, terminal-binding, session-opening, payout, offline-sync, settlement, reconciliation — listés dans `role-flows.md`
