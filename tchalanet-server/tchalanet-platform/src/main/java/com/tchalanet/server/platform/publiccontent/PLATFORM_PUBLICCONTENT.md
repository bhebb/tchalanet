# Platform Capability `platform.publiccontent` — Public Content

## Rôle

Gérer et exposer des articles de contenu (news, annonces) par surface applicative.  
Chaque surface (home public, dashboards admin/tenant/POS) a son propre flux de contenus.

**Ce module fait** :
- Publier et gérer des articles de contenu par surface
- Exposer des listes de contenus récents par surface via `PublicContentApi`
- Gérer le cycle de vie DRAFT → PUBLISHED → ARCHIVED

**Ce module ne fait pas** :
- Pages légales / CGU / mentions légales (hors scope de ce module)
- Assets statiques (images, logos) — via CDN
- Contenu multilingue avancé (localization non gérée ici)

---

## Enums

### `PublicContentStatus`

| Valeur | Sens |
|---|---|
| `DRAFT` | Brouillon — non visible |
| `PUBLISHED` | Publié — visible par les consommateurs |
| `ARCHIVED` | Archivé — masqué |

### `PublicContentSourceType`

| Valeur | Sens |
|---|---|
| `INTERNAL` | Contenu créé manuellement en admin |
| `EXTERNAL_RSS` | Contenu importé depuis un flux RSS |

### `PublicContentSurface`

| Valeur | Consommateur |
|---|---|
| `PUBLIC_HOME` | Site public / page d'accueil |
| `TENANT_ADMIN_DASHBOARD` | Dashboard administrateur tenant |
| `PLATFORM_ADMIN_DASHBOARD` | Dashboard super-admin |
| `POS_DASHBOARD` | Dashboard POS cashier |

---

## API — `PublicContentApi`

```java
List<PublicContentItemView> listPublicHomeNews(int limit)
List<PublicContentItemView> listTenantAdminDashboardNews(int limit)
List<PublicContentItemView> listPlatformAdminDashboardNews(int limit)
List<PublicContentItemView> listPosDashboardNews(int limit)
```

---

## Modèle — `PublicContentItemView`

| Champ | Type | Sens |
|---|---|---|
| `id` | `UUID` | — |
| `title` | `String` | Titre de l'article |
| `content` | `String` | Corps du contenu |
| `imageUrl` | `String?` | URL image optionnelle |
| `sourceUrl` | `String?` | URL source (pour RSS) |
| `sourceType` | `PublicContentSourceType` | Origine |
| `publishedAt` | `Instant` | Date de publication |

---

## Intégration PageModel

- `PublicNewsProvider` (source : `public_news`) consomme `listPublicHomeNews`
- Les providers des dashboards admin consomment les méthodes correspondantes

---

## Règles

- Pas de RLS (contenu global — sauf surfaces POS qui peuvent être tenant-scoped à préciser)
- Modifications auditées via `platform.audit`
- `EXTERNAL_RSS` : import planifié, pas temps réel

---

## Références

- PageModel providers : `features/pagemodel/FEATURE_PAGEMODEL.md §Current dynamic sources`
