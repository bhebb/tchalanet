# Feature PageModel (BFF)

> Orchestrateur BFF qui résout un JSON PageModel par (scope, tenant, role, logical_page_id) et agrège des blocs dynamiques via des providers.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/pagemodel.md`

---

## 1. Rôle & objectifs

- Résoudre `PageModelResponse` (currentLang, langs, pageModel, dynamic, i18n, issues).
- Appeler des providers pour remplir `dynamic` (draw/news/plans/etc.).
- Mobile-first; respect tokens/thèmes.

---

## 2. Endpoints (tenant/public/admin)

- GET `/tenant/pagemodel/{logicalPageId}` — privé/admin selon role.
- GET `/public/pagemodel/{logicalPageId}` — pages publiques si applicable.

Retour: `ApiResponse<PageModelResponse>`.

---

## 3. Handlers appelés & agrégation

- Appelle les handlers des domaines: sales/payout/draw/news selon widgets.
- Agrège les vues retournées en `dynamic`.
- Mapping via MapStruct, DTO `XxxResponse`.

---

## 4. Pagination & cache

- Pagination via `@TchPaging TchPageRequest` quand une liste est exposée.
- Cache court pour résolutions fréquentes (L1 Caffeine); L2 Redis si pages publiques (TTL court).

---

## 5. Sécurité & permissions

- `@Secured` selon scope; `@PreAuthorize` pour permissions fines si nécessaires.
- Context via `@CurrentContext TchRequestContext`.

---

## 6. Notes techniques

- DTO output suffixe `Response`; input suffixe `Request`.
- IDs wrappers (`TenantId`, etc.).
- Ne pas introduire de logique métier; rester orchestrateur.
