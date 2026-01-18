# Catalog — Domaine fonctionnel (référentiels)

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/public/draw/slots` — liste des slots actifs
- Components/widgets :
  - (ex) `ResultSlotList`
- i18n namespaces :
  - (ex) `catalog.*`, `publicdraw.*`

### Mobile (POS)

- Écrans :
  - (ex) “Choisir slot”
- Offline/Sync :
  - (ex) cache long des slots

### API (contrats)

- Endpoints SDR (admin):
  - (ex) `/_sdr/resultslots` — CRUD slots
- Notes :
  - SDR admin-only, `@RepositoryRestResource`

> Source of truth :
>
> - Backend : Domain files sous `tchalanet-server/src/main/java/com/tchalanet/server/catalog/.../DOMAIN_*.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Game: `tchalanet-server/src/main/java/com/tchalanet/server/catalog/game/DOMAIN_GAME.md`
- Pricing: `tchalanet-server/src/main/java/com/tchalanet/server/catalog/pricing/DOMAIN_PRICING.md`
- DrawResult: `tchalanet-server/src/main/java/com/tchalanet/server/catalog/drawresult/DOMAIN_DRAWRESULT.md`
- ResultSlot: `tchalanet-server/src/main/java/com/tchalanet/server/catalog/resultslot/DOMAIN_RESULTSLOT.md`
