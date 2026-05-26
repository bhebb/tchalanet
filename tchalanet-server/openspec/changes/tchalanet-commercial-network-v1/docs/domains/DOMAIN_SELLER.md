# DOMAIN_SELLER — Seller / Machann V1

## Rôle

`core.seller` représente l'identité métier du machann, c'est-à-dire le vendeur transactionnel.

Décision :

```text
Seller = Agent métier / machann / vendeur transactionnel.
User = authentification.
Cashier = écran/flow.
Outlet = canal/lieu/institution.
```

## Agrégats et modèles

### Seller

Agrégat minimal.

```text
seller
  id
  tenant_id
  user_id nullable
  code nullable
  display_name
  status: ACTIVE / SUSPENDED / INACTIVE
  audit columns
```

Règles : `user_id` est un lien optionnel, pas une fusion; un seller peut exister avant d'avoir un login; un user peut être lié plus tard à un seller.

### SellerOutletAssignment

Rattachement historisé.

```text
seller_outlet_assignment
  id
  tenant_id
  seller_id
  outlet_id
  starts_at
  ends_at nullable
  status: ACTIVE / ENDED / SUSPENDED
  audit columns
```

Règles : ne jamais écraser l'ancien assignment; un changement d'outlet ferme l'ancien assignment puis en ouvre un nouveau; les anciens tickets gardent l'ancien `seller_assignment_id`.

### SellerCommissionPolicy

Commission simple V1.

```text
seller_commission_policy
  id
  tenant_id
  seller_id
  type: NONE / PERCENT / FIXED_PER_TICKET / FIXED_PLUS_PERCENT
  base: GROSS_SALES / NET_SALES / PROFIT / TICKET_COUNT
  rate_percent nullable
  fixed_amount nullable
  currency nullable
  starts_at
  ends_at nullable
  status
  audit columns
```

Sales snapshotte la policy applicable au moment de la vente. Pas de `core.compensation` générique en V1.

## Ce que core.seller ne fait PAS

- pas d'authentification ;
- pas de rôles/permissions ;
- pas de limite de vente ;
- pas de prepaid ledger financier ;
- pas de création ticket ;
- pas de settlement ;
- pas de payout ;
- pas de UI cashier.

## Limites / prepaid

Décision actuelle : pas de credit/prepaid ledger en core.seller V1. Les limites de vente sont portées par `core.limitpolicy` avec scope SELLER.

Si plus tard prepaid signifie compte rechargeable avec topup/debit/balance_after, il faudra extraire un `SellerCredit` ledger séparé.

## Commands

```text
CreateSellerCommand
LinkSellerToUserCommand
UnlinkSellerUserCommand
AssignSellerToOutletCommand
EndSellerAssignmentCommand
SuspendSellerCommand
ReactivateSellerCommand
SetSellerCommissionPolicyCommand
EndSellerCommissionPolicyCommand
```

## Queries

```text
ResolveSellerForOperationQuery(userId, outletId, sessionId)
GetSellerQuery(sellerId)
ListSellersByOutletQuery(outletId)
ListSellerAssignmentsQuery(sellerId)
GetSellerCommissionPolicyAtQuery(sellerId, soldAt)
```

## Events

```text
SellerCreatedEvent
SellerLinkedToUserEvent
SellerAssignedToOutletEvent
SellerAssignmentEndedEvent
SellerSuspendedEvent
SellerCommissionPolicyChangedEvent
```

## Onboarding

Temps 1 — Tenant onboarding : créer tenant, admin tenant user, outlets initiaux. Ne pas créer automatiquement de seller.

Temps 2 — Seller onboarding : admin tenant crée Seller, assignment outlet, éventuellement user/login, éventuellement commission policy.

Temps 3 — Vie du seller : changement d'outlet = fermer assignment courant puis ouvrir le nouveau; suspension; changement commission = fermer ancienne policy puis créer nouvelle.

## Stale snapshot / revalidation

`ResolveSellerForOperationQuery` est une photo. La vente critique doit revalider transactionnellement avant de créer le ticket : seller ACTIVE, assignment still open, assignment matches outlet, commission policy applicable.
