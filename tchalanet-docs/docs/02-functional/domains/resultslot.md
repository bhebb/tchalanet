# Catalog: ResultSlot — Domaine fonctionnel

## Rôle

Slots globaux (provider, timezone, drawTime, daysOfWeek).

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages: `/public/draw/slots`
- Widgets: `ResultSlotList`
- i18n: `publicdraw.*`, `catalog.*`

### Mobile (POS)

- Écrans: choix slot (si applicable)
- Offline: cache de slots (TTL long)

### API (contrats)

- SDR admin: `/_sdr/resultslots`
- Notes: lecture read-mostly; admin-only pour CRUD

## Pointeurs (source of truth near-code)

- Backend: `../../99-links/_ref/server/domains/catalog/resultslot/DOMAIN_RESULTSLOT.md`
