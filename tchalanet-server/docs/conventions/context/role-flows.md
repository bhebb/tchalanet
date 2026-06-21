# Role Flows — Contexte par acteur

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — référence croisée tous composants  
> **Règle** : Un flow compose des sous-flows par liens. Il ne les recopie pas.  
> **Dernière mise à jour** : 2026-06-20

---

## Principe de composition

Un flow ne redécrit pas en détail un sous-flow déjà documenté ailleurs.  
Il nomme le sous-flow, indique pourquoi il est requis, pointe vers la source canonique,  
et précise uniquement ce qui est spécifique au flow courant.

---

## Sous-flows canoniques

| Sous-flow | Source canonique |
|---|---|
| Provisioning SellerTerminal | `tchalanet-docs/docs/02-functional/flows/seller-onboarding.md` |
| Vente ticket | `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` |
| Payout terrain | `tchalanet-docs/docs/02-functional/flows/payout-field-flow.md` |
| Offline grant / sync | `tchalanet-docs/docs/02-functional/flows/offline-sync.md` |
| Sélection Admin POS | `tchalanet-docs/docs/02-functional/flows/admin-pos-selection.md` |
| Vérification ticket public | `tchalanet-docs/docs/02-functional/flows/verify-ticket.md` |
| Pipeline résultats | `tchalanet-docs/docs/02-functional/flows/draw-execution.md` |
| Settlement | `tchalanet-docs/docs/02-functional/flows/settlement.md` |
| Réconciliation | `tchalanet-docs/docs/02-functional/flows/reconciliation.md` |

---

## Acteur : SellerTerminal (POS)

`TchActorType.SELLER_TERMINAL` — identifié par `ACTOR_SELLER_TERMINAL`.

**Prérequis avant toute action POS :**

1. Firebase id_token valide → `IdentityProviderApi.mapVerifiedToken()` résout le `seller_terminal`
2. `TchAccessContextPipelineFilter` : statut `ACTIVE` (sinon 403), `mustChangePin` vérifié
3. `TchRequestContext` construit avec `actorType=SELLER_TERMINAL`, `sellerTerminalId`, `permissionKeys`
4. Si `mustChangePin = true` → seul `POST /me/change-pin` est autorisé ; toutes les actions de vente sont bloquées

**Pour les actions sensibles (sell, payout, offline) :**

```
actorType = SELLER_TERMINAL
  → sellerTerminalId résolu (non null)
  → statut seller_terminal = ACTIVE
  → mustChangePin = false
  → permission accordée (ex: ticket.sell, payout.execute)
  → gates action-specific (cutoff, limits, promotions, idempotency)
```

Il n'y a **pas** de terminal binding, pas de session POS, pas d'outlet requis.  
Le SellerTerminal est lui-même le contexte opérationnel.

**Aucun `OperationalContext` (Terminal/Outlet/Session) n'est requis.**

---

## Acteur : Tenant Admin (APP_USER)

`TchActorType.APP_USER` avec `roleCodes` contenant `TENANT_ADMIN`.

**Prérequis :**

1. Firebase id_token valide → `app_user` résolu
2. `TchAccessContextPipelineFilter` : statut `app_user = ACTIVE`, roles/permissions chargés
3. `TchRequestContext` : scope `ADMIN`, `actorType=APP_USER`, `roleCodes={"TENANT_ADMIN"}`, `permissionKeys`

**Actions disponibles selon permissions :**
- Gestion SellerTerminals → `seller_terminal.manage`, `seller_terminal.block`, `seller_terminal.pin.reset`
- Gestion users → `user.manage`
- Gestion outlets → `outlet.manage`
- Settings → `settings.manage`

---

## Acteur : Admin en mode POS

Un `TENANT_ADMIN` peut accéder aux fonctions POS en sélectionnant explicitement un SellerTerminal.

**Prérequis supplémentaires :**

1. Même prérequis que Tenant Admin normal
2. Sélection POS explicite — `POST /tenant/cashier/operational-context/select`
3. SellerTerminal sélectionné : actif et non bloqué

→ Voir [admin-pos-selection](../../../02-functional/flows/admin-pos-selection.md)

---

## Acteur : Super-admin

`TchActorType.APP_USER` avec `roleCodes` contenant `SUPER_ADMIN`.

**Prérequis :**

1. Firebase id_token valide → `app_user` résolu avec rôle `SUPER_ADMIN`
2. Override tenant explicite si nécessaire :
   ```
   X-Tenant-Id: <tenant-code-or-id>
   ```
3. Audit obligatoire — chaque override est tracé

**Actions disponibles :**
- Platform ops
- Override tenant
- Platform audit

→ Voir [`request-context.md`](./request-context.md#tenant-policy-par-scope)

---

## Acteur : System / Batch

**Prérequis :**

1. Pas de HTTP request — pas de pipeline filter
2. Contexte construit explicitement via `BatchTchContextBinder`
3. `actorType = SYSTEM`
4. Tenant explicite si job tenant-scoped
5. Pas de SellerTerminal context sauf cas documenté

Sous-flows :
- Batch execution
- Scheduler tick → lance batch ou commande, bind contexte explicite
- Reconciliation
- Settlement
- Analytics recompute

---

## Acteur : Public (anonymous)

**Prérequis :**

1. `TchRequestContext` construit — scope `PUBLIC`
2. Tenant public par défaut (`tchalanet`)
3. JWT optionnel

**Actions disponibles :**
- Vérification ticket public → voir [verify-ticket](../../../02-functional/flows/verify-ticket.md)
- Résultats tirage public → voir [draw-execution](../../../02-functional/flows/draw-execution.md)
- Page model public

---

## Matrice de synthèse

| Acteur | TchActorType | Firebase → | Request Context | Actions clés |
|---|---|---|---|---|
| SellerTerminal POS | `SELLER_TERMINAL` | `seller_terminal` | `TENANT`, sellerTerminalId | Sell, payout, offline |
| Tenant Admin | `APP_USER` | `app_user` | `ADMIN`, roleCodes | Gestion SellerTerminals, users, settings |
| Admin POS | `APP_USER` | `app_user` | `ADMIN`, sélection explicite | Sell via sélection SellerTerminal |
| Super-admin | `APP_USER` | `app_user` | `PLATFORM`, override tenant | Platform ops, override |
| System/Batch | `SYSTEM` | — | Construit explicitement | Batch, scheduler, replay |
| Public | — | — | `PUBLIC` | Verify ticket, draw results |
