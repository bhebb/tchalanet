# Sales — Domaine fonctionnel

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/app/sales/tickets` — émission/annulation
  - (ex) `/public/ticket/:code` — vérification publique
- Components/widgets :
  - (ex) `TicketIssueForm`, `TicketHistoryTable`
- i18n namespaces :
  - (ex) `sales.*`, `ticket.*`

### Mobile (POS)

- Écrans :
  - (ex) “Vendre ticket” — formulaire + scan code public
- Offline/Sync :
  - (ex) pending sync si offline

### API (contrats)

- Endpoints :
  - (ex) `POST /api/v1/tenant/sales/tickets`
  - (ex) `POST /api/v1/tenant/sales/tickets/{id}:cancel`
  - (ex) `GET /api/v1/public/ticket/{publicCode}`
- Notes :
  - scopes tenant/public; idempotency (Issue)

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/sales/DOMAIN_SALES.md`
