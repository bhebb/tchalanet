# OpenSpec — Context Packs Index

**Router IA** — pointe vers les fichiers canoniques, ne recopie pas les règles.  
Charger 2-4 packs max par feature. Charger l'inutile est une erreur.

---

## Packs globaux (toujours charger)

| Pack | Fichier | Statut |
|---|---|---|
| Version guard | `openspec/context/05-version-guard.md` | ✅ existe |
| Non-négociables backend | `tchalanet-server/openspec/context/10-non-negotiables.md` | ✅ existe |

---

## Packs techniques backend (charger si pertinent)

Ces packs vivent dans `tchalanet-server/openspec/context/` :

| Pack | Fichier | Statut |
|---|---|---|
| Modulith global rules | `tchalanet-server/openspec/context/70-modulith-global-rules.md` | ✅ existe |
| Idempotency | `tchalanet-server/openspec/context/25-idempotency.md` | ✅ existe |
| Ticket codes | `tchalanet-server/openspec/context/26-ticket-codes.md` | ✅ existe |
| Common rules | `tchalanet-server/openspec/context/72-common-rules.md` | ✅ existe |
| API response rules | `tchalanet-server/openspec/context/76-api-response-rules.md` | ✅ existe |
| Persistence rules | `tchalanet-server/openspec/context/77-persistence-rules.md` | ✅ existe |
| Platform rules | `tchalanet-server/openspec/context/78-platform-rules.md` | ✅ existe |
| Request context rules | `tchalanet-server/openspec/context/79-request-context-rules.md` | ✅ existe |
| Core rules | `tchalanet-server/openspec/context/80-core-rules.md` | ✅ existe |
| Features rules | `tchalanet-server/openspec/context/81-features-rules.md` | ✅ existe |
| Bus rules | `tchalanet-server/openspec/context/82-bus-rules.md` | ✅ existe |
| Cache rules | `tchalanet-server/openspec/context/83-cache-rules.md` | ✅ existe |
| Typed IDs rules | `tchalanet-server/openspec/context/84-typed-ids-rules.md` | ✅ existe |
| Pagination rules | `tchalanet-server/openspec/context/85-pagination-rules.md` | ✅ existe |
| Naming rules | `tchalanet-server/openspec/context/74-naming-rules.md` | ✅ existe |
| Catalog rules | `tchalanet-server/openspec/context/75-catalog-rules.md` | ✅ existe |
| Operational context | `tchalanet-server/openspec/context/73-operational-context-rules.md` | ✅ existe |
| Maven module rules | `tchalanet-server/openspec/context/71-maven-module-rules.md` | ✅ existe |
| Security flows | `tchalanet-server/openspec/context/90-security-flows-guide.md` | ✅ existe |

---

## Packs domaine (charger si la feature touche la logique domaine)

| Pack | Fichier | Statut |
|---|---|---|
| Domain sales | `tchalanet-server/openspec/context/71-domain-sales.md` | ✅ existe |

---

## Packs manquants (à créer — Phase 3)

Ces packs sont référencés dans des specs mais n'existent pas encore :

| Pack | Destination attendue |
|---|---|
| Frontend rules (Angular/Nx) | `openspec/context/30-frontend-rules.md` ou `tchalanet-web/openspec/context/` |
| Mobile rules (Flutter) | `openspec/context/40-mobile-rules.md` ou `tchalanet-mobile/openspec/context/` |
| Edge service rules | `openspec/context/50-edge-service-rules.md` ou `tchalanet-edge-service/openspec/context/` |
| Infra rules | `openspec/context/60-infra-rules.md` ou `tchalanet-infra/openspec/context/` |
| Domain draw | `tchalanet-server/openspec/context/70-domain-draw.md` |
| Domain payout | `tchalanet-server/openspec/context/72-domain-payout.md` |
| Domain ledger | `tchalanet-server/openspec/context/73-domain-ledger.md` |
| Domain limit policy | `tchalanet-server/openspec/context/74-domain-limitpolicy.md` |
| Glossary | `openspec/context/90-glossary.md` |

---

## Règle de chargement

```
Toujours :
  - 05-version-guard.md
  - tchalanet-server/openspec/context/10-non-negotiables.md

Puis seulement si pertinent :
  - 1 pack technique max
  - 1 pack domaine max
  - jamais tous les packs
```

---

## Sources canoniques

Les context packs pointent vers ces sources, ne les copient pas :

- Backend : `tchalanet-server/docs/` et `src/**/DOMAIN_*.md`
- Web : `tchalanet-web/docs/` et README libs
- Mobile : `tchalanet-mobile/docs/`
- Edge : `tchalanet-edge-service/docs/`
- Infra : `tchalanet-infra/docs/`
- Portail : `tchalanet-docs/docs/`
- Versions : `VERSIONS.md`
