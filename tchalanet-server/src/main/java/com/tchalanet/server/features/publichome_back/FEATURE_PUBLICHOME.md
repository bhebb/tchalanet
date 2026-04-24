# Feature PublicHome (BFF)

> BFF public pour la page d’accueil (widgets: news, draws, highlights).

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/publichome.md`

---

## 1. Rôle & objectifs

- Exposer les données publiques nécessaires à la home.
- Agréger plusieurs providers (news/draws/etc.).

---

## 2. Endpoints (public)

- GET `/public/home` — charge les widgets publics.

Retour: `ApiResponse<HomeResponse>`.

---

## 3. Handlers appelés & agrégation

- Providers: news, draws, plans, etc.
- Agrégation: `HomeResponse`.

---

## 4. Pagination & cache

- Pas de pagination.
- Cache L2 Redis recommandé (TTL court).

---

## 5. Sécurité

- Public: rate-limit; `noindex` si requis.

---

## 6. Notes techniques

- DTO suffixes; wrappers ID (si tenant override).
- Pas de logique métier.
