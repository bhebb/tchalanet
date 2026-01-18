# Feature News (BFF)

> BFF pour exposer les actualités (public/tenant/admin), agrégées depuis sources internes et externes, avec cache et contrôles d’affichage.
>
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/news.md`

---

## 1. Rôle & objectifs

- Exposer un flux paginé d’actus (public/tenant/admin).
- Agréger des sources internes (admin) et externes (LotteryDaily RSS) et normaliser.
- Permettre de cacher une news (sans suppression) et rafraîchir contrôlé.

---

## 🧩 Structure du slice

```
features/news
├── shared/
│    ├── LotteryNewsModels.java
│    ├── NewsStatus.java
│    ├── port/
│    │    ├── NewsProviderPort.java
│    │    └── NewsCachePort.java
│    └── service/
│         ├── InternalNewsService.java
│         ├── ExternalNewsService.java
│         ├── HiddenNewsService.java
│         └── NewsAggregationService.java
├── publicview/
│    ├── PublicNewsService.java
│    └── PublicNewsController.java
├── admin/
│    ├── AdminNewsService.java
│    └── AdminNewsController.java
```

---

## ✨ Modèle principal

`LotteryNewsModels.LotteryNewsFeedSnapshot`

```java
record LotteryNewsFeedSnapshot(
    Instant fetchedAt,
    List<LotteryNewsArticle> articles
) {}
```

Sources:

- interne → `InternalNewsService`
- externe → feed RSS (ROME) via `NewsProviderPort`

---

## 🔤 Statuts éditoriaux

```java
public enum NewsStatus {
  DRAFT,
  PUBLISHED,
  ARCHIVED
}
```

- DRAFT: visible seulement en admin
- PUBLISHED: visible publiquement (si non hidden)
- ARCHIVED: plus visible (équiv. hidden permanent)

---

## 🗂 Services

- `ExternalNewsService`: fetch RSS via `NewsProviderPort`, parse XML, cache (`newsExternalKey`) 24h
- `InternalNewsService`: snapshot interne (`newsInternalKey`), `save()`, `changeStatus()`, `findPublished()` (V1: sans DB)
- `HiddenNewsService`: cache IDs cachés (TTL 24h), `hide(id)`, `show(id)`
- `NewsAggregationService`: assemblage public (internes `PUBLISHED` puis externes, filtrage `hiddenIds`, tri `publishedAt DESC`)

---

## 2. Endpoints (tenant/public/admin)

- GET `/public/news` — flux public.
- GET `/tenant/news` — flux tenant (auth requise).

Retour: `ApiResponse<TchPage<NewsResponse>>`.

---

## 3. Handlers appelés & agrégation

- Queries `ListNewsQuery` (source abstraite).
- Mapping via MapStruct.

---

## 4. Pagination & cache

- `@TchPaging TchPageRequest` obligatoire.
- Cache L1; L2 Redis optionnel pour public (TTL court).

---

## 5. Sécurité

- Public: pas de JWT; rate-limit.
- Tenant: `@Secured` rôles; context via `@CurrentContext`.

---

## 6. Notes techniques

- DTO suffixes; wrappers ID.
- Pas de logique métier.

---

## 📝 Notes futures (V2+)

- Détails article → `GET /api/public/news/{id}`
- Rendu Markdown / rich HTML contrôlé
- Pagination avancée
