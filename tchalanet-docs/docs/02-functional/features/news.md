# Feature: News — Fonctionnel

## Rôle

Exposer un flux d’actualités agrégées (internes/externe), avec cache et contrôle d’affichage.

## Invariants

- Orchestration only; pas de logique métier

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/public/news`, `/app/news`
- Widgets: NewsList, NewsCard
- i18n: `news.*`

### Mobile (POS)

- Écrans: news POS
- Offline: cache court

### API (contrats)

- Endpoints:
  - `GET /api/v1/public/news`
  - `GET /api/v1/tenant/news`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/news/FEATURE_NEWS.md`
