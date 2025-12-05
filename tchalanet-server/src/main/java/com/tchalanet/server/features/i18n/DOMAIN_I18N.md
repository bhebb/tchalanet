# I18N

Domaine : i18n (traduction + overrides par tenant)

---

## 🎯 Objectifs du domaine

Permettre :

- d'afficher les traductions de base (fichiers JSON : `fr.json`, `en.json`, `ht.json`)
- d'appliquer des overrides par tenant (en base de données)
- d'exposer une API pour le public et pour les tenants
- d’utiliser Spring Data REST pour le CRUD admin global

Ce domaine gère la **configuration i18n effective** (base + overrides tenant).

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

---

## 🧱 Repository Spring Data REST

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

→ Permet un CRUD complet pour super-admin **sans écrire de controller**.

---

## 🔤 Service : `BaseI18nLoader`

Charge les fichiers :

- `assets/i18n/en.json`
- `assets/i18n/fr.json`
- `assets/i18n/ht.json`

Retourne un `Map<String, String>`.

Charge une seule fois par langue (**cache in-memory**).

---

## 🧠 Service : `I18nConfigService`

Contrat :

```java
public interface I18nConfigService {
  Map<String, String> getEffectiveMessages(String locale, @Nullable UUID tenantId);
}
```

### Règle de merge (ordre de priorité)

1. base JSON (classpath)
2. overrides tenant (DB)  
   → messages effectifs

Les overrides **remplacent** les clés correspondantes.

`Spring Cache` recommandé pour `(tenantId, locale)`.

---

## 🧭 Service : `TenantI18nOverrideService`

Utilisé pour :

- lister les overrides d’un tenant (avec pagination)
- l’écran admin tenant

Méthodes principales :

```java
Page<I18nOverrideEntity> pageByTenant(UUID tenantId, Pageable page);

Page<I18nOverrideEntity> pageByTenantAndLocale(
    UUID tenantId,
    String locale,
    Pageable page
);
```

---

## 🌐 API exposée

### 1. Public (pas de tenant)

```http
GET /api/public/i18n/{locale}
```

→ retourne **uniquement** le JSON de base (pas d’overrides DB).

### 2. Privé (tenant authentifié)

```http
GET /api/app/i18n/{locale}
```

→ combine :

- base JSON
- overrides du tenant

---

## 🔐 Accès

| API                           | Accès                              |
| ----------------------------- | ---------------------------------- |
| `/api/public/i18n/...`        | tout le monde                      |
| `/admin/i18n-overrides`       | super-admin (via Spring Data REST) |
| `/api/app/i18n-overrides/...` | admins tenant                      |
| `/api/app/i18n/...`           | tenant authentifié                 |

---

## 📝 Notes futures (V2+)

- Support des namespaces (comme `nav.*`, `footer.*`, `cta.*`)
- Interface admin pour visualiser les overrides
- Validation côté serveur (clé existante dans la base JSON)
