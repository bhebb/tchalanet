# Feature: PublicHome — Fonctionnel

## Rôle

Exposer la page d’accueil publique agrégée (widgets: news, draws, highlights).

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/public/home`
- Widgets: HomeWidgets

### Mobile (POS)

- Écrans: home public minimal

### API (contrats)

- Endpoints:
  - `GET /api/v1/public/home`

## Pointeurs (near-code)

- Backend FEATURE: `tchalanet-server/src/main/java/com/tchalanet/server/features/publichome/FEATURE_PUBLICHOME.md`
