# Session POS — Ouverture, fermeture et validation

> Session de vente liée à un terminal/outlet pour une plage de service.  
> Domaine : `core.session` — `core/session/DOMAIN_SESSION.md`  
> Feature : `features.cashier` — `CashierSessionController` · `CashierHomeService`

---

## Pourquoi

Une session POS est la fenêtre comptable d'une période de vente. Elle lie un terminal, un outlet et un vendeur sur une plage de temps. Sans session ouverte, les actions de vente, payout et offline sont bloquées.

La session est aussi la source de vérité pour le solde cash déclaré en début et fin de service.

---

## Pré-requis

- Seller authentifié (JWT valide)
- Terminal bindé et actif → [terminal-binding](./terminal-binding.md)
- Seller assigné à l'outlet → [seller-onboarding](./seller-onboarding.md)
- Contexte opérationnel sélectionné (terminal + outlet) → [operational-context](../../../tchalanet-server/docs/conventions/context/operational-context.md)

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

## Validation POS — flags et blockers

Avant d'autoriser la vente, l'app appelle `GET /tenant/cashier/home` (ou `GET /tenant/cashier/readiness`). Le serveur construit un `CashierReadinessResponse` qui expose **l'ensemble des flags d'état** du poste.

### Structure de la réponse

```json
{
  "ready": false,
  "attentionLevel": "BLOCKED",
  "blockers": [
    {
      "type": "OPERATIONAL_CONTEXT",
      "titleKey": "pos.readiness.operational_context.title",
      "messageKey": "pos.readiness.operational_context.message",
      "params": { "missing": ["OUTLET", "TERMINAL"] }
    }
  ],
  "badges": [],
  "notifications": []
}
```

### `attentionLevel`

| Valeur | Signification |
|---|---|
| `BLOCKED` | Au moins un blocker — vente impossible |
| `CARD` | Pas de blocker mais des notifications à afficher |
| `NONE` | Tout est propre — prêt à vendre |
| `BADGE` | Badge non-bloquant (compteur) |

### Blockers critiques (bloquent la vente)

| `type` | Cause | Champ `params` | Étape requise |
|---|---|---|---|
| `OPERATIONAL_CONTEXT` | Outlet ou terminal absent/non trusted | `missing: ["OUTLET"]` · `missing: ["TERMINAL"]` · `missing: ["OUTLET","TERMINAL"]` | `HomeRequiredStepType.SELECT_OPERATIONAL_CONTEXT` |
| `SESSION_CLOSED` | Aucune session `OPEN` pour ce terminal | `{}` | `HomeRequiredStepType.OPEN_SESSION` |

### `HomeRequiredStep` — guide l'action suivante

Quand `ready = false`, la home POS affiche un `requiredStep` :

| `HomeRequiredStepType` | Affiché quand | Action suggérée |
|---|---|---|
| `SELECT_OPERATIONAL_CONTEXT` | Contexte manquant ou non trusted | Naviguer vers `/operational-context/select` |
| `OPEN_SESSION` | Contexte OK mais pas de session ouverte | Naviguer vers `/session/open` |

### Badges et notifications (non-bloquants)

| `type` | `attentionLevel` | Cause | Action |
|---|---|---|---|
| `PREVIOUS_UNPAID_PAYOUTS` | `BADGE` (compteur) + `CARD` (notification) | Claims OPEN ou BLOCKED datant d'avant aujourd'hui | `VIEW_PAYOUTS_TO_PROCESS` |

Les badges n'empêchent pas la vente. Ils signalent une attention requise.

### Vérification des champs manquants (detail)

`operationalContext.missing` est calculé ainsi :

```
missing = []
if outletId absent dans TchRequestContext → missing.add("OUTLET")
if terminalId absent dans TchRequestContext → missing.add("TERMINAL")
trusted = hint.trustedForSensitiveOperation()
ready = missing.isEmpty() && trusted
```

Un contexte peut avoir outlet + terminal mais `trusted = false` si la source est insuffisante → blocker `OPERATIONAL_CONTEXT` quand même.

---

## Flow : Ouverture manuelle

```
1. App appelle GET /tenant/cashier/home (ou /readiness)
   → Analyser blockers et requiredStep

2. Si OPERATIONAL_CONTEXT manquant :
   → POST /tenant/cashier/operational-context/select
   → Recharger home

3. Si SESSION_CLOSED :
   GET /tenant/cashier/session/current?terminalId=<id>
   → 204 No Content  ← pas de session ouverte
   → 200 { sessionId, status:"OPEN", ... }  ← session déjà ouverte

   POST /tenant/cashier/session/open
   { outletId, terminalId, openingFloat }
   → 201 { sessionId, status:"OPEN", openedAt, openingFloat }
   → OpenSalesSessionCommand → SalesSessionOpenedEvent

4. Stocker sessionId localement → X-Tch-Sales-Session-Id sur chaque requête

5. GET /tenant/cashier/home → ready:true → vente autorisée
```

**Règles :**
- Un terminal ne peut avoir qu'une session `OPEN` à la fois — doublon → 409
- `openingFloat` = montant cash déclaré en caisse au début (peut être 0)
- `sessionId` doit être stocké localement et envoyé dans `X-Tch-Sales-Session-Id`

---

## Flow : Fermeture manuelle (fin de service)

```
POST /tenant/cashier/session/close
  { sessionId, closingAmount, reason }
  → 200 { sessionId, status:"CLOSED", closedAt, closingAmount }
  → CloseSalesSessionCommand → SalesSessionClosedEvent
```

**Règles :**
- `closingAmount` = montant cash déclaré à la fermeture (comparé au total calculé par `SessionCashCalculator`)
- `reason` = motif libre (ex: "end-of-shift", "terminal issue")
- La session passe `CLOSED` — le batch peut la passer à `FINALIZED` ensuite

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

## Validation opération (avant chaque action sensible)

`ValidateSalesSessionForOperationQuery` est appelé pour chaque opération sell/payout/offline :

```java
ValidateSalesSessionForOperationQuery(
    tenantId, salesSessionId, terminalId, outletId, sellerUserId, operation
)
→ ValidatedSalesSessionOperationView { sessionId, status, openedAt, closedAt, ... }
```

Si la session n'est plus `OPEN` au moment de la vente → rejet avec issue `SESSION_CLOSED`.

---

## Invariants

- Un terminal → au plus une session `OPEN`
- `openedBy` et `closedBy` sont tracés pour audit
- Après `CLOSED`, la session ne peut pas revenir `OPEN`
- Le `closingAmount` est déclaratif — indépendant du total système

---

## Références

- Domaine : `core/session/DOMAIN_SESSION.md`
- Contexte opérationnel : [operational-context](../../../tchalanet-server/docs/conventions/context/operational-context.md)
- Sell ticket : [sell-ticket](./sell-ticket.md)
- Settlement après fermeture : [settlement](./settlement.md)
