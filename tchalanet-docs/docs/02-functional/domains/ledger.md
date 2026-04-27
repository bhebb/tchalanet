# Ledger — Domaine fonctionnel

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/app/admin/ledger` — lecture entries, filtres
- Components/widgets :
  - (ex) `LedgerEntriesTable`
- i18n namespaces :
  - (ex) `ledger.*`

### Mobile (POS)

- Écrans :
  - (ex) “Historique caisse” (si exposé)
- Offline/Sync :
  - (ex) lecture locale si cache

### API (contrats)

- Endpoints :
  - (ex) `GET /api/v1/admin/ledger/entries?refType=...&refId=...`
  - (ex) `GET /api/v1/admin/ledger/entries?from=...&to=...&type=...`
- Notes :
  - admin-only

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/ledger/DOMAIN_LEDGER.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/ledger/DOMAIN_LEDGER.md`
