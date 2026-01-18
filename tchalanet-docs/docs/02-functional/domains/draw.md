# Draw — Domaine fonctionnel

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/public/draw/slots` — slots actifs
  - (ex) `/public/results` — historique
- Components/widgets :
  - (ex) `PublicDrawList`, `LatestResultsPanel`
- i18n namespaces :
  - (ex) `draw.*`, `publicdraw.*`

### Mobile (POS)

- Écrans :
  - (ex) “Consulter dernier résultat”
- Offline/Sync :
  - (ex) mise en cache courte

### API (contrats)

- Endpoints :
  - (ex) `GET /api/v1/public/results/latest`
  - (ex) `GET /api/v1/public/results`
- Notes :
  - rate-limit, noindex pour public

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/draw/DOMAIN_DRAW.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/draw/DOMAIN_DRAW.md`
