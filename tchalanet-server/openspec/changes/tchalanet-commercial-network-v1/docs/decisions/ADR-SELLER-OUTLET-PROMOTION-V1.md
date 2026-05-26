# ADR — Commercial Network V1 : Seller, Outlet, Promotion

## Status

Accepted — implémenté (`core.agent` renommé en `core.seller`, zones/hiérarchie/mandates supprimés, V130__core_seller.sql, migration SQL OK).

## Contexte

Le concept initial `agent` devenait trop gros : vendeur transactionnel, institution partenaire, banque, outlet, prepaid, commission, paiement, sous-agent, réseau commercial.

## Décision

Pour V1, remplacer le concept flou `agent` par `core.seller`.

### Définitions

```text
User        Identité d'authentification, Keycloak / platform.identity.
Seller      Identité métier du machann / vendeur transactionnel.
Cashier     Feature UI / BFF / écran POS utilisé par le seller.
Outlet      Lieu, institution, partenaire, banque, point mobile ou canal de vente.
Sales       Domaine qui crée le ticket et snapshotte la vérité commerciale au moment de la vente.
LimitPolicy Domaine qui contrôle les plafonds / limites de vente, y compris scope SELLER.
Promotion   Domaine qui configure des effets commerciaux. Sales les matérialise en snapshots.
```

## Règle courte

```text
User s'authentifie.
Seller vend.
Cashier est l'écran.
Outlet est le canal.
Sales snapshotte la vérité de vente.
LimitPolicy encadre ce que le seller peut vendre.
Promotion configure, Sales matérialise.
```

## Conséquences

### On crée `core.seller`

Responsable de : Seller, SellerOutletAssignment historisé, SellerCommissionPolicy simple, résolution seller pour une opération.

Non responsable de : auth, rôles, permissions, limites, prepaid ledger, compensation engine, paiement seller, création ticket, settlement, payout, UI cashier.

### On garde `core.outlet`

Responsable de : outlet physique, mobile, institutionnel, partenaire, type/kind d'outlet, statut opérationnel, zone.

### On garde `features.cashier`

Responsable de l'expérience UI / POS. Orchestration via CommandBus/QueryBus, pas d'invariants métier.

### On garde Promotion V1 limitée

Promotion V1 reste limitée à 3 effets : `FREE_GAME_LINE`, `BOOST_ODDS`, `WAIVE_CHARGE`.

## Ce qu'on ne fait pas maintenant

- `core.compensation`
- `core.partner`
- `core.agentpayment`
- moteur complet de rémunération
- prepaid ledger financier
- partner settlement
- multi-level commission
