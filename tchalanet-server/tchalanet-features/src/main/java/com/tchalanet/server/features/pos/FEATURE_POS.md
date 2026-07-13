# Feature Cashier

> **Surface** : POS mobile / web cashier — BFF vendeur transactionnel  
> **Scope** : `features.cashier` — orchestration, pas de logique métier propre  
> **Domaines appelés** : `core.sales`, `core.sellerterminal`, `catalog.game`, `catalog.drawchannel`  
> **Acteur** : `SELLER_TERMINAL` (ou `APP_USER` avec Admin POS sélection)

---

## Rôle

BFF du terminal de vente. Agrège plusieurs domaines pour l'écran POS/mobile.
Ne possède pas de logique métier — orchestre via `CommandBus` / `QueryBus`.

---

## Prérequis pour opérer

1. `TchActorType.SELLER_TERMINAL` authentifié (Firebase JWT valide)
2. `seller_terminal.status = ACTIVE` (sinon 403)
3. `seller_terminal.mustChangePin = false` (sinon `requiredStep: MUST_CHANGE_PIN`)

Il n'y a **pas** de terminal binding, pas de session POS, pas d'outlet requis pour vendre.

Pour l'Admin POS : `APP_USER` + sélection explicite via `POST /cashier/operational-context/select`.

---

## Surfaces

| Surface | Endpoint principal | Notes |
|---|---|---|
| POS mobile | `GET /tenant/cashier/home` | BFF compact, pas PageModel |
| Cashier web | PageModel `DASHBOARD_CASHIER_WEB` | Source `cashier_dashboard` |

### Home POS (`/tenant/cashier/home`)

Retourne en une seule réponse :
- **Identité** : sellerTerminalId, code, nom, tenant
- **Statut** : canSell (boolean), requiredStep si bloquant
- **Tirage principal** : label, cutoff, statut
- **Action principale** : sell
- **Notices** : avertissements non-bloquants (ex: payouts à traiter)

### `requiredStep` — seul cas bloquant

| `HomeRequiredStepType` | Cause | Action requise |
|---|---|---|
| `MUST_CHANGE_PIN` | `mustChangePin = true` (après provisioning ou reset admin) | `POST /seller-terminal/me/change-pin` |

---

## Endpoints

### Tirages et jeux

```http
GET /tenant/cashier/draws/available   ← filtre status=OPEN pour le panier
GET /tenant/cashier/games/available   ← libellés vendeur, betTypes, betOptions
```

`/tenant/cashier/games/available` expose uniquement les jeux actifs et visibles POS du tenant.
Le flow `POST /tenant/sales/preparations` réapplique la même frontière serveur :
jeu actif/visible, option active/visible POS, et min/max stake configurés pour le tenant.

### Tickets

```http
POST /tenant/sales/preparations                ← prépare le panier et retourne preparationId
POST /tenant/sales/preparations/{id}/confirm   ← confirme une préparation, Idempotency-Key obligatoire
POST /tenant/cashier/tickets/verify            ← scanner URL publique ou code brut
POST /tenant/cashier/tickets/{id}/cancel
GET  /tenant/cashier/tickets                   ← tickets de la session en cours
GET  /tenant/cashier/tickets/{id}
POST /tenant/cashier/tickets/{id}/print        ← générer reçu (PDF / ESC-POS)
POST /tenant/cashier/tickets/{id}/send         ← email / SMS
```

**Prepare** — utilise le flow `core.sales` prepare et retourne `preparationId` + statut.

**Confirm** — confirme une préparation existante.
Retourne `outcome` : `ACCEPTED` / `REJECTED` / `PENDING_APPROVAL`
→ Sur `ACCEPTED` : afficher `backup.displayCode` immédiatement (garantie offline client)  
→ `Idempotency-Key` UNE FOIS par panier — re-poster la même clé si timeout

**Verify** — accepte `https://tchalanet.com/v/TCH-8F4K-29PL` ou `TCH-8F4K-29PL`  
→ Retourne statut, sévérité, clés i18n, actions (`EXECUTE_PAYOUT`, etc.)

### Admin POS — sélection explicite (APP_USER only)

```http
POST   /tenant/cashier/operational-context/select   ← { sellerTerminalId }
GET    /tenant/cashier/operational-context/current
DELETE /tenant/cashier/operational-context
```

---

## Flow typique d'un SellerTerminal

```
1. Authentification Firebase (PIN) → firebase id_token
2. GET /tenant/cashier/home
   → Si mustChangePin: true → aller à change-pin flow
   → Si canSell: true → prêt
3. GET /tenant/cashier/draws/available
4. GET /tenant/cashier/games/available
5. POST /tenant/sales/preparations         ← préparer le panier et garder preparationId
6. POST /tenant/sales/preparations/{id}/confirm ← confirmer et afficher displayCode
7. POST /tenant/cashier/tickets/{id}/print ou /send
```

---

## Frontières

`features.cashier` ne doit pas :
- calculer les invariants de vente, limite ou settlement
- écrire directement dans les repositories `core.*`
- appeler `tchalanet-edge-service` directement
- dupliquer la logique métier de `core.sales`
- gérer des sessions POS ou du binding terminal

---

## Références

- Intégration mobile (Flutter) : [`MOBILE_FLOW.md`](./MOBILE_FLOW.md)
- Contexte opérationnel : `docs/conventions/context/operational-context.md`
- Sell ticket flow : `tchalanet-docs/docs/02-functional/flows/sell-ticket.md`
- Domaine SellerTerminal : `core.sellerterminal/DOMAIN_SELLERTERMINAL.md`
- Print ticket : `tchalanet-docs/docs/02-functional/domains/sales/print-ticket/`
