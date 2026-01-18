# Documentation Backend (tchalanet-server)

...existing code...

## 🔧 Détails Techniques

...existing code...

### Conventions backend (nouveaux documents)

- Pagination (canonique): `conventions/PAGINATION.md`
- Web API (conventions & style): `conventions/web_api.md`
- Cache (ajout/modification): `conventions/cache.md`
- Batch (jobs/schedulers): `conventions/batch.md`

Ces documents sont des références projet pour normaliser les contrôleurs, la pagination, et les aspects transverses cache/batch.

---

## 🧱 Entities vs Persistence — Séparation requise

- Entities (JPA) **MUST** rester dans `infra/persistence` de chaque domaine et étendre les bases (`BaseEntity` / `BaseTenantEntity`).
- Persistence **MUST** implémenter des ports du domaine (adapters JPA/JDBC) sans faire fuiter les entities vers le domaine.
- Le domaine **MUST NOT** dépendre des entities JPA.
- Les mappings Domain ↔ Entity sont gérés via **MapStruct**.

Voir: `persistence.md` (bases JPA, Envers, UUID vs wrappers) et `DOMAIN_TEMPLATE.md`.

---

## ✅ Conformité (Context, Audit, RLS)

- Context: Conforme — `TchContextFilter` publie `TchRequestContext`, usage via `@CurrentContext` et `TchContext.currentOrNull()`. Règles de résolution tenant centralisées.
- Audit: Conforme — Envers activé via base classes; audit domain events via `AfterCommit` + `DomainEventPublisher` selon besoin.
- RLS: Conforme — `RlsAwareDataSource` applique `set_config('app.current_tenant', ...)` et `app.deleted_visibility`; reset via proxy; policies DB appliquées.

Références longues: `rls.md`, `ARCHITECTURE.md`, `API_RESPONSE_STANDARDIZATION.md`.

---

## 📖 Documentation Métier

...existing code...

## 🔗 Références Externes

...existing code...

## 🚀 Quick Start

...existing code...

## 📊 Statut Documentation

- ✅ Pas de doublons majeurs (nettoyés 2026-01-17)
- ✅ Conventions backend ajoutées (pagination, web API, cache, batch)
- ✅ Entities vs Persistence clarifié (séparation requise)
- ✅ Context, Audit, RLS: conformes
- 🟡 Migration conventions/ vers docs longues: partielle (liens en place)

---

**Maintenu par**: équipe Tchalanet  
**Dernière mise à jour**: 2026-01-17
