# Session POS — Ouverture et fermeture

> Session de vente liée à un terminal/outlet pour une plage de service.  
> Domaine : `core.session` — `tchalanet-server/tchalanet-core/.../core/session/DOMAIN_SESSION.md`  
> Feature : `features.cashier` — `CashierSessionController`

---

## Pourquoi

Une session POS est la fenêtre comptable d'une période de vente. Elle lie un terminal, un outlet et un vendeur sur une plage de temps. Sans session ouverte, les actions de vente, payout et offline sont bloquées.

La session est aussi la source de vérité pour le solde cash déclaré en début et fin de service.

---

## Pré-requis

- Seller authentifié (JWT valide)
- Terminal bindé et actif → [terminal-binding](./terminal-binding.md)
- Seller assigné à l'outlet → [seller-onboarding](./seller-onboarding.md)
- Contexte opérationnel sélectionné (terminal + outlet)

---

## États d'une session

```
OPEN → CLOSED → FINALIZED
     ↘ CANCELLED
```

| État | Signification |
|---|---|
| `OPEN` | Session active — ventes autorisées |
| `CLOSED` | Fermée par le vendeur — comptabilisée |
| `CANCELLED` | Annulée sans avoir eu de ventes |
| `FINALIZED` | Traitée par le batch de settlement/réconciliation |

Un seul `OPEN` par terminal à la fois.

---

## Flow : Ouverture manuelle

```
GET /tenant/cashier/session/current?terminalId=<id>
  → 204 No Content  ← pas de session ouverte
  → 200 { sessionId, status:"OPEN", ... }  ← session déjà ouverte

POST /tenant/cashier/session/open
  { outletId, terminalId, openingFloat }
  → 201 { sessionId, status:"OPEN", openedAt, openingFloat }
  → OpenSalesSessionCommand → SalesSessionOpenedEvent
```

**Règles :**
- Un terminal ne peut avoir qu'une session `OPEN` à la fois — doublon → 409
- `openingFloat` = montant cash déclaré en caisse au début (peut être 0)
- Le `sessionId` retourné doit être stocké localement et envoyé dans `X-Tch-Sales-Session-Id`

---

## Flow : Fermeture manuelle (fin de service)

```
POST /tenant/cashier/session/close
  { sessionId, closingAmount, reason }
  → 200 { sessionId, status:"CLOSED", closedAt, closingAmount }
  → CloseSalesSessionCommand → SalesSessionClosedEvent
```

**Règles :**
- `closingAmount` = montant cash déclaré à la fermeture (pour comparaison avec attendu)
- `reason` = motif libre (ex: "end-of-shift", "terminal issue")
- La session passe `CLOSED` — le batch peut la `FINALIZED` ensuite

---

## Fermeture automatique

`SalesSessionAutoCloser` ferme les sessions encore `OPEN` dans ces cas :

| Déclencheur | Mécanisme |
|---|---|
| Outlet désactivé | `CloseOutletOpenSalesSessionsCommand` |
| Sessions trop anciennes | `CloseDueSalesSessionsCommand` (scheduler) |
| Terminal désactivé/unbindé | Fermeture cascadée |

Les sessions auto-fermées ont `reason = "auto_closed"`.

---

## Invariants

- Un terminal → au plus une session `OPEN`
- `openedBy` et `closedBy` sont tracés pour audit
- Après `CLOSED`, la session ne peut pas revenir `OPEN`
- Le `closingAmount` est déclaratif — le système calcule indépendamment le total attendu (via `SessionCashCalculator`)

---

## Références

- Domaine : `core/session/DOMAIN_SESSION.md`
- Contexte opérationnel requis : [operational-context](../../../tchalanet-server/docs/conventions/context/operational-context.md)
- Sell ticket : [sell-ticket](./sell-ticket.md)
- Settlement après fermeture : [settlement](./settlement.md)
