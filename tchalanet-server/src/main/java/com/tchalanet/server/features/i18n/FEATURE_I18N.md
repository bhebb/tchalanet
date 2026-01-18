# Feature i18n (BFF)

> BFF pour exposer les ressources de traduction (namespaces, overrides tenant) au web/mobile.

---

## 🎯 Objectifs

- Afficher les traductions de base (JSON: `fr.json`, `en.json`, `ht.json`).
- Appliquer des overrides par tenant (DB) et exposer une API public/tenant.
- Supporter CRUD admin global via Spring Data REST.

---

## 🧩 Structure du slice

```text
common/persistence/
├── I18nOverrideEntity.java
└── I18nOverrideRepository.java   (exposé en Spring Data REST)

features/i18n/
├── TenantI18nOverrideService.java
├── I18nConfigService.java
├── I18nConfigServiceImpl.java
├── BaseI18nLoader.java (+ impl)
├── PublicI18nController.java
└── TenantI18nController.java
```

---

## 🗄 Table : `i18n_override`

```sql
CREATE TABLE i18n_override (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version    bigint NOT NULL DEFAULT 0,

  tenant_id  uuid NOT NULL REFERENCES tenant(id),
  locale     text NOT NULL,          -- 'fr', 'en', 'ht'
  i18n_key   text NOT NULL,          -- ex: 'nav.home.label'
  i18n_value text NOT NULL,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE UNIQUE INDEX ux_i18n_override_tenant_locale_key
  ON i18n_override(tenant_id, locale, i18n_key);
```

RLS: tenant-scoped (BaseTenantEntity). Audit via Envers recommandé.

---

## 📚 Repository Spring Data REST

```java
@RepositoryRestResource(
  path = "admin/i18n-overrides",
  collectionResourceRel = "i18n-overrides"
)
public interface I18nOverrideRepository
    extends JpaRepository<I18nOverrideEntity, UUID> {

  Page<I18nOverrideEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<I18nOverrideEntity> findByTenantIdAndLocaleIgnoreCase(
      UUID tenantId,
      String locale,
      Pageable pageable
  );
}
```

Usage: CRUD complet pour super-admin **sans écrire de controller** (SDR sous `/_sdr/...` si isolé).

---

## 🔤 Service : `BaseI18nLoader`

- Charge `assets/i18n/en.json`, `fr.json`, `ht.json`.
- Retourne `Map<String, String>`.
- Cache in-memory (une fois par langue).

---

## 🧠 Service : `I18nConfigService`

Contrat:

```java
public interface I18nConfigService {
  Map<String, String> getEffectiveMessages(String locale, @Nullable UUID tenantId);
}
```

Règle de merge (priorité):

1. base JSON (classpath)
2. overrides tenant (DB)
   → messages effectifs (overrides remplacent les clés).

Spring Cache recommandé pour `(tenantId, locale)`.

---

## 🧭 Service : `TenantI18nOverrideService`

- lister overrides d’un tenant (pagination)
- écran admin tenant

Méthodes principales:

```java
Page<I18nOverrideEntity> pageByTenant(UUID tenantId, Pageable page);
Page<I18nOverrideEntity> pageByTenantAndLocale(UUID tenantId, String locale, Pageable page);
```

---

## 🌐 API exposée

### Public (pas de tenant)

`GET /api/public/i18n/{locale}` → retourne uniquement le JSON de base.

### Privé (tenant authentifié)

`GET /api/app/i18n/{locale}` → combine base JSON + overrides du tenant.

Retour: `ApiResponse<I18nBundleResponse>`.

---

## 🔐 Accès

| API                           | Accès                 |
| ----------------------------- | --------------------- |
| `/api/public/i18n/...`        | tout le monde         |
| `/admin/i18n-overrides`       | super-admin (via SDR) |
| `/api/app/i18n-overrides/...` | admins tenant         |
| `/api/app/i18n/...`           | tenant authentifié    |

---

## 4. Pagination & cache

- Pas de pagination côté public bundle.
- Cache L2 Redis recommandé (TTL court) par `(tenant, lang, namespace)`.

---

## 5. Sécurité

- Public: namespaces autorisés.
- Tenant: `@Secured` selon rôle; context via `@CurrentContext`.

---

## 6. Notes techniques

- DTO suffixes `Response`/`Request`.
- Wrappers ID pour tenant; pas de UUID brut.
- Respect ApiResponse + conventions backend.

---

## 1. Rôle & objectifs

- Fournir les bundles i18n organisés par namespace (snake_case).
- Gérer les overrides tenant et fusion avec defaults.

---

## 2. Endpoints

- GET `/public/i18n/{lang}/{namespace}` — public pages.
- GET `/tenant/i18n/{lang}/{namespace}` — overrides tenant (auth requise).

Retour: `ApiResponse<I18nBundleResponse>`.

---

## 3. Handlers appelés & agrégation

- Queries: `GetI18nBundleQuery` (tenant-aware).
- Agrégation: merge defaults + overrides.

---

## 4. Pagination & cache

- Pas de pagination.
- Cache L2 Redis recommandé (TTL court) par `(tenant, lang, namespace)`.

---

## 5. Sécurité

- Public: namespaces autorisés.
- Tenant: `@Secured` selon rôle; context via `@CurrentContext`.

---

## 6. Notes techniques

- DTO suffixes `Response`/`Request`.
- Wrappers ID pour tenant.

---

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/i18n.md`
