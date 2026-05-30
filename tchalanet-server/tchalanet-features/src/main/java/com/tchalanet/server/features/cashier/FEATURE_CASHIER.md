# Feature Cashier — Guide du Seller

> **Surface** : POS mobile / web cashier — BFF vendeur transactionnel  
> **Scope** : `features.cashier` — orchestration, pas de logique métier propre  
> **Guide d'implémentation mobile** : [`MOBILE_FLOW.md`](./MOBILE_FLOW.md) (endpoints, payloads, idempotency)  
> **Domaines appelés** : `core.sales`, `core.session`, `core.terminal`, `core.outlet`, `core.payout`, `core.offlinesync`, `catalog.game`, `catalog.drawchannel`

---

## Rôle

BFF du vendeur terrain. Agrège plusieurs domaines pour l'écran POS/mobile.
Ne possède pas de logique métier — orchestre via `CommandBus` / `QueryBus`.

---

## Pré-requis pour opérer

1. Seller actif assigné à un outlet → [`seller-onboarding`](../../../../../../tchalanet-docs/docs/02-functional/flows/seller-onboarding.md)
2. Terminal bindé → [`terminal-binding`](../../../../../../tchalanet-docs/docs/02-functional/flows/terminal-binding.md)
3. Contexte opérationnel trusted → [`role-flows`](../../../../../../tchalanet-docs/docs/02-functional/flows/role-login-flow.visual.html)

---

## Surfaces

| Surface | Endpoint principal | Notes |
|---|---|---|
| POS mobile | `GET /tenant/cashier/home` | Endpoint compact, pas PageModel |
| Cashier web | PageModel `DASHBOARD_CASHIER_WEB` | Source `cashier_dashboard` |

### Home POS (`/tenant/cashier/home`)

Retourne en une seule réponse :
- **Identité** : seller, outlet, terminal, tenant
- **Contexte opérationnel** : statut (ready / not-ready), source, champs manquants
- **Session** : ouverte/fermée, openedAt, compteurs tickets/ventes
- **Tirage principal** : label, cutoff, statut
- **Action principale** : sell / ouvrir session / configurer contexte
- **Notices** : blocages et avertissements opérationnels

---

## Endpoints

### Contexte opérationnel

```http
GET    /tenant/cashier/operational-context/current
POST   /tenant/cashier/operational-context/select    ← terminalId, outletId, salesSessionId
DELETE /tenant/cashier/operational-context
```

### Session de vente

```http
GET  /tenant/cashier/session/current
POST /tenant/cashier/session/open    ← outletId, terminalId, openingFloat
POST /tenant/cashier/session/close   ← sessionId, closingAmount
```

### Tirages et jeux

```http
GET /tenant/cashier/draws/available   ← filtre status=OPEN pour le panier
GET /tenant/cashier/games/available   ← libellés vendeur, betTypes, betOptions, hints
```

> Le POS envoie la saisie brute (`gameCode`, `betType`, `selection`).  
> `core.sales` canonicalise. Ne jamais afficher les noms d'enum techniques au vendeur.

### Tickets

```http
POST /tenant/cashier/tickets/preview           ← valider le panier avant vente
POST /tenant/cashier/tickets/sell              ← Idempotency-Key obligatoire
POST /tenant/cashier/tickets/verify            ← scanner URL publique ou code brut
POST /tenant/cashier/tickets/{id}/cancel
GET  /tenant/cashier/tickets                   ← tickets de la session
GET  /tenant/cashier/tickets/{id}
POST /tenant/cashier/tickets/{id}/print        ← générer reçu (PDF / ESC-POS)
POST /tenant/cashier/tickets/{id}/send         ← email / SMS
```

**Preview** — retourne `decision` : `ACCEPTABLE` / `REQUIRES_CHANGES` / `REJECTED_FINAL`

**Sell** — retourne `outcome` : `ACCEPTED` / `REJECTED` / `PENDING_APPROVAL`  
→ Sur `ACCEPTED` : afficher `backup.displayCode` immédiatement (garantie offline client)  
→ `Idempotency-Key` UNE FOIS par panier — re-poster la même clé si timeout

**Verify** — accepte `https://tchalanet.com/v/TCH-8F4K-29PL` ou `TCH-8F4K-29PL`  
→ Retourne statut, sévérité, clés i18n, actions (`EXECUTE_PAYOUT`, etc.)

### Offline

```http
GET  /tenant/cashier/offline/grant/current    ← grant offline actif
POST /tenant/cashier/offline/submissions      ← soumettre ventes offline
```

---

## Readiness

`CashierReadinessResponse` — badges non-bloquants sur la home :
- `PREVIOUS_UNPAID_PAYOUTS` — paiements ouverts, action `VIEW_PAYOUTS_TO_PROCESS`

Le cashier n'est pas propriétaire de la vérité payout — il affiche une attention seulement.

---

## Flow typique d'une session vendeur

```
1. Login → Request Context
2. GET /tenant/cashier/operational-context/current
     → Si not trusted : POST /select (terminal + outlet + session)
3. POST /tenant/cashier/session/open
4. GET  /tenant/cashier/draws/available
5. GET  /tenant/cashier/games/available
6. POST /tenant/cashier/tickets/preview    ← valider avant chaque ligne
7. POST /tenant/cashier/tickets/sell       ← afficher displayCode immédiatement
8. POST /tenant/cashier/tickets/{id}/print ou /send
9. POST /tenant/cashier/session/close
```

---

## Frontières

`features.cashier` ne doit pas :
- calculer les invariants de vente, limite, payout ou settlement
- écrire directement dans les repositories `core.*`
- appeler `tchalanet-edge-service` directement
- dupliquer la logique métier de `core.sales`

---

## Références

- Intégration mobile (Flutter) : [`MOBILE_FLOW.md`](./MOBILE_FLOW.md)
- Guide vendeur (non-technique) : [`SELLER_GUIDE.md`](./SELLER_GUIDE.md)
- Contexte opérationnel : `docs/conventions/context/operational-context.md`
- Sell ticket flow : `tchalanet-docs/docs/02-functional/flows/sell-ticket.md`
- Print ticket : `tchalanet-docs/docs/02-functional/domains/sales/print-ticket/`
