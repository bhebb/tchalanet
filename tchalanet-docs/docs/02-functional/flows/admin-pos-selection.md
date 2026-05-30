# Admin POS Selection — Flow

> Un administrateur tenant accède aux fonctions POS en sélectionnant explicitement un contexte opérationnel.  
> Référence : `tchalanet-server/docs/conventions/context/operational-context.md`  
> Rôle concerné : `TENANT_ADMIN` (et `SUPER_ADMIN` avec override tenant)

---

## Pourquoi

Un `TENANT_ADMIN` a accès à l'interface d'administration. Pour réaliser des opérations POS (vente, payout, vérification terrain), il doit sélectionner explicitement un terminal et un outlet — ce n'est jamais automatique.

La source `ADMIN_SELECTION` distingue ce contexte du contexte `CASHIER` standard. Les mêmes validations s'appliquent ensuite.

---

## Différence avec le flow Seller POS

| | Seller POS | Admin POS |
|---|---|---|
| Rôle | `CASHIER` | `TENANT_ADMIN` |
| Source opérationnelle | `CASHIER_LOGIN` | `ADMIN_SELECTION` |
| Sélection | Automatique si terminal unique | **Toujours explicite** |
| Accès admin UI | Non | Oui (en parallèle) |
| Contrainte terminal | Terminal assigné au seller | N'importe quel terminal actif du tenant |

---

## Flow

```
Admin authentifié (scope ADMIN)
  └─ Request Context construit — TchContextFilter
     └─ Pas d'Operational Context par défaut

Sélection explicite POS :
  POST /tenant/cashier/operational-context/select
  {
    "outletId":       "<outletId>",
    "terminalId":     "<terminalId>",
    "salesSessionId": "<sessionId>"   ← session déjà ouverte ou null
  }
  → 200 { terminalId, outletId, salesSessionId, source:"ADMIN_SELECTION", trust:"TRUSTED" }

Optionnel — ouvrir une session si nécessaire :
  POST /tenant/cashier/session/open
  { outletId, terminalId, openingFloat }
  → sessionId à utiliser dans X-Tch-Sales-Session-Id

Actions POS disponibles après sélection :
  → vente : POST /tenant/cashier/tickets/sell
  → payout : POST /tenant/cashier/payout/...
  → vérification : POST /tenant/cashier/tickets/verify

Fin d'intervention POS :
  DELETE /tenant/cashier/operational-context
  → 204 — contexte effacé, retour mode admin pur
```

---

## Validations appliquées (identiques au Seller POS)

```
terminal existe / actif / non bloqué       [core.terminal]
outlet existe / actif                      [core.outlet]
session existe / ouverte / correspond      [core.session]
permission accordée                        [platform.accesscontrol]
```

→ Fail-fast order complet : `operational-context.md §fail-fast-order`

---

## Invariants

- L'admin ne peut sélectionner qu'un terminal appartenant à son tenant
- `salesSessionId` peut être null si aucune session n'est encore ouverte — l'admin doit en ouvrir une explicitement
- Le contexte `ADMIN_SELECTION` est tracé dans les opérations POS (audit)
- `DELETE /operational-context` efface la sélection — les appels suivants reviennent en mode admin normal

---

## Références

- Contexte opérationnel : `docs/conventions/context/operational-context.md`
- Ouverture session : [session-opening](./session-opening.md)
- Flow rôles : `docs/conventions/context/role-flows.md §Admin en mode POS`
