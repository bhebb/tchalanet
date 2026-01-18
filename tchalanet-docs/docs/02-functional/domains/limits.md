# Limits — Domaine fonctionnel

## Rôle

Mettre en place des garde‑fous de vente/payout (plafonds, compteurs, exposition) pour protéger le système et respecter les règles commerciales/risque.

## Invariants (métier)

- Les limites s’appliquent selon une période (`per_ticket`, `per_draw`, `per_day`).
- Les agrégations respectent une portée (`AGENT`, `OUTLET`, `ZONE`, `RANGE`, `TENANT`).
- Les décisions sont déterministes et explicites (allow/deny + raison).

## Concepts (glossaire)

- Période: fenêtre d’application (ticket/draw/jour)
- Portée d’agrégation: où cumuler (agent/outlet/zone/range/tenant)
- Dimension: `line` / `ticket` / `selection` / `total`
- Canonisation selection_key: 2D/3D/marriage/lotto patterns
- Bet types: `MATCH_1_2D`, `MARRIAGE_2D2D`, `LOTTO3_3D`, etc.

## Scénarios supportés

- Plafond par sélection (par ticket ou par tirage)
- Plafond total par tirage/journée
- Plafond d’exposition payout potentiel
- Minimum/maximum stake par ligne/ticket
- Limite d’annulations par jour

---

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/app/sales` — validation avant émission (notice en cas de limite)
  - (ex) `/app/admin/limits` — gestion/règles limites
- Components/widgets :
  - (ex) `LimitNoticeBanner`, `LimitRulesEditor`
- i18n namespaces :
  - (ex) `limits.*`, `sales.*`

### Mobile (POS)

- Écrans :
  - (ex) “Vendre ticket” — blocage/notice lorsque limite atteinte
- Offline/Sync :
  - (ex) refus de vente hors ligne si fact requis; messages clairs

### API (contrats)

- Endpoints :
  - (ex) `POST /api/v1/tenant/sales/tickets` — validation via LimitPolicy en amont
  - (ex) `GET /api/v1/admin/limits` — lecture des règles actives
- Notes :
  - scopes tenant/admin, validations, idempotency côté vente

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/limitpolicy/DOMAIN_LIMITPOLICY.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

---

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/limitpolicy/DOMAIN_LIMITPOLICY.md`
- OpenSpec pack: `openspec/context/30-backend.md` (règles backend), `openspec/context/72-domain-payout-ledger.md` (références croisées)
