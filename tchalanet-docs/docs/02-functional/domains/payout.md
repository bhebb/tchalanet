# Payout — Domaine fonctionnel

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages / routes :
  - (ex) `/app/payouts` — liste des claims
  - (ex) `/app/payouts/:id` — détail + payer/reverser
- Components/widgets :
  - (ex) `PayoutListWidget`, `PayoutDetailsPanel`
- i18n namespaces :
  - (ex) `payout.*`, `ledger.*`

### Mobile (POS)

- Écrans :
  - (ex) “Encaisser gain” — scan ticket → claim
- Offline/Sync :
  - (ex) restrictions si offline (amount limits)

### API (contrats)

- Endpoints :
  - (ex) `POST /api/v1/tenant/payout/claims`
  - (ex) `POST /api/v1/tenant/payout/claims/{claimId}/payments`
  - (ex) `POST /api/v1/tenant/payout/payments/{paymentId}:reverse`
- Notes :
  - idempotency (Post payment), optimistic lock `version`

> Source of truth :
>
> - Backend : `tchalanet-server/src/main/java/com/tchalanet/server/core/payout/DOMAIN_PAYOUT.md`
> - Web : `apps/...` / `libs/.../README.md`
> - Mobile : (chemins quand prêts)

## Pointeurs (source of truth near-code)

- Règles et modèle backend: `99-links/_ref/server/domains/core/payout/DOMAIN_PAYOUT.md`
