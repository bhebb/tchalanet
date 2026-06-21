# Admin POS Selection — Flow

> Un administrateur tenant accède aux fonctions POS en sélectionnant explicitement un SellerTerminal.  
> Référence : `tchalanet-server/docs/conventions/context/operational-context.md`  
> Acteur concerné : `APP_USER` avec rôle `TENANT_ADMIN` (ou `SUPER_ADMIN` avec override tenant)

---

## Pourquoi

Un `TENANT_ADMIN` a accès à l'interface d'administration. Pour réaliser des opérations POS (vente, payout), il doit sélectionner explicitement un `SellerTerminal` — ce n'est jamais automatique.

La source `ADMIN_SELECTION` distingue ce contexte du flow SellerTerminal direct.

---

## Différence avec le flow SellerTerminal POS

| | SellerTerminal POS | Admin POS |
|---|---|---|
| TchActorType | `SELLER_TERMINAL` | `APP_USER` |
| Source opérationnelle | Intrinsèque (acteur = contexte) | `ADMIN_SELECTION` (toujours explicite) |
| Auth | Firebase PIN | Firebase (compte admin) |
| Accès admin UI | Non | Oui (en parallèle) |
| SellerTerminal requis | L'acteur lui-même | N'importe quel SellerTerminal actif du tenant |

---

## Flow

```
Admin authentifié (scope ADMIN, actorType=APP_USER)
  └─ TchRequestContext construit — pas de sellerTerminalId par défaut

Sélection explicite d'un SellerTerminal :
  POST /tenant/cashier/operational-context/select
  {
    "sellerTerminalId": "<UUID>"
  }
  → 200 { sellerTerminalId, source:"ADMIN_SELECTION", trust:"TRUSTED" }

Actions POS disponibles après sélection :
  → vente : POST /tenant/cashier/tickets/sell
  → payout : POST /tenant/cashier/payout/...
  → vérification : POST /tenant/cashier/tickets/verify

Fin d'intervention POS :
  DELETE /tenant/cashier/operational-context
  → 204 — contexte effacé, retour mode admin pur
```

---

## Validations appliquées

```
sellerTerminalId non null (sélection effectuée)
seller_terminal.status = ACTIVE (non bloqué)
seller_terminal.tenant_id = tenant courant
permission accordée               [platform.accesscontrol]
gates action-specific             [core.sales / core.payout]
```

→ Fail-fast order complet : `operational-context.md §fail-fast-order`

---

## Invariants

- L'admin ne peut sélectionner qu'un SellerTerminal appartenant à son tenant
- Le contexte `ADMIN_SELECTION` est tracé dans les opérations POS (audit)
- `DELETE /operational-context` efface la sélection — les appels suivants reviennent en mode admin normal

---

## Références

- Contexte opérationnel : `docs/conventions/context/operational-context.md`
- Flow rôles : `docs/conventions/context/role-flows.md §Acteur: Admin en mode POS`
