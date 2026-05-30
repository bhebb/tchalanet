# Role Flows — Contexte par rôle

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — référence croisée tous composants  
> **Règle** : Un flow compose des sous-flows par liens. Il ne les recopie pas.  
> **Dernière mise à jour** : 2026-05-30

---

## Principe de composition

Un flow ne redécrit pas en détail un sous-flow déjà documenté ailleurs.  
Il nomme le sous-flow, indique pourquoi il est requis, pointe vers la source canonique,  
et précise uniquement ce qui est spécifique au flow courant.

**Interdit :**
```
Seller POS flow:
1. Décrire toute la création du seller.
2. Décrire toute la procédure de binding terminal.
3. Décrire toute l'ouverture de session.
4. Puis décrire sell.
```

**Correct :**
```
Seller POS flow:
1. Request Context construit.
2. Seller actif — voir seller-onboarding.md
3. Terminal bindé et trusted — voir terminal-binding.md
4. Session POS ouverte — voir session-opening.md
5. Ce flow valide seulement que seller/terminal/outlet/session correspondent pour l'action.
```

---

## Sous-flows canoniques

| Sous-flow | Source canonique |
|---|---|
| Création / onboarding seller | `tchalanet-docs/docs/02-functional/flows/seller-onboarding.md` |
| Terminal binding / device trust | `tchalanet-docs/docs/02-functional/flows/terminal-binding.md` |
| Ouverture de session POS | `tchalanet-docs/docs/02-functional/flows/session-opening.md` |
| Sélection Admin POS | `tchalanet-docs/docs/02-functional/flows/admin-pos-selection.md` |
| Vérification ticket public | `tchalanet-docs/docs/02-functional/flows/verify-ticket.md` |
| Vente ticket | `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` |
| Payout terrain | `tchalanet-docs/docs/02-functional/flows/payout-field-flow.md` |
| Offline grant / sync | `tchalanet-docs/docs/02-functional/flows/offline-sync.md` |
| Pipeline résultats | `tchalanet-docs/docs/02-functional/flows/draw-execution.md` |
| Settlement | `tchalanet-docs/docs/02-functional/flows/settlement.md` |
| Réconciliation | `tchalanet-docs/docs/02-functional/flows/reconciliation.md` |

---

## Rôle : Seller (POS)

**Prérequis avant d'arriver au flow d'action :**

1. Request Context construit — `TchContextFilter`
2. App user bootstrappé — `UserBootstrapFilter`
3. Seller actif et onboardé → voir [seller-onboarding](../../../02-functional/flows/seller-onboarding.md)
4. Terminal bindé et trusted → voir [terminal-binding](../../../02-functional/flows/terminal-binding.md)
5. Session POS ouverte → voir [session-opening](../../../02-functional/flows/session-opening.md)

**Pour chaque action sensible (sell, payout, offline grant) :**

```
trusted operational context requis
  → terminal existe / tenant / non bloqué          [core.terminal]
  → outlet existe / tenant / actif                 [core.outlet]
  → session existe / ouverte / match               [core.session]
  → permission accordée                            [platform.accesscontrol]
  → gates action-specific                          [core.sales / core.payout]
```

Voir fail-fast order complet : [`operational-context.md`](./operational-context.md#fail-fast-order)

---

## Rôle : Tenant Admin (normal)

**Prérequis :**

1. Request Context construit — scope `ADMIN`
2. Tenant requis depuis contexte authentifié
3. Pas d'Operational Context requis

**Actions disponibles selon permissions :**
- Gestion users → `tenant user management`
- Gestion outlets → `outlet management`
- Gestion terminals → `terminal management`
- Settings → `settings management`

Pas d'accès POS sans sélection explicite.

---

## Rôle : Admin en mode POS

**Prérequis supplémentaires par rapport à Admin normal :**

1. Request Context construit — scope `ADMIN`
2. Sélection POS explicite — `POST /tenant/me/operational-context/select`  
   Source : `ADMIN_SELECTION` — **pas automatique**
3. Mêmes validations terminal/outlet/session que Seller ensuite

→ Voir [admin-pos-selection](../../../02-functional/flows/admin-pos-selection.md)

---

## Rôle : Super-admin

**Prérequis :**

1. Request Context construit — scope `SUPER_ADMIN`
2. Override tenant explicite si nécessaire :
   ```
   X-Tch-Tenant-Override: <tenant-code-or-id>
   X-Tch-Override-Reason: <raison obligatoire>
   ```
3. Audit obligatoire — chaque override est tracé
4. Pas d'Operational Context POS sauf sélection explicite

**Actions disponibles :**
- Platform ops
- Tenant override
- Platform audit

→ Voir tenant policy : [`request-context.md`](./request-context.md#tenant-policy-par-scope)

---

## Rôle : System / Batch

**Prérequis :**

1. Pas de HTTP request — pas de `TchContextFilter`
2. Contexte construit explicitement via `BatchTchContextBinder`
3. `actorUserId` = `SYSTEM`
4. Tenant explicite si job tenant-scoped
5. Pas d'Operational Context sauf cas très spécifique documenté

**Cas particulier — Offline replay :**
```
actorUserId  = SYSTEM
sellerUserId = cashier original (préservé pour audit métier)
```

→ Voir [`request-context.md §Batch / Scheduler`](./request-context.md#batch--scheduler)

Sous-flows :
- Batch execution
- Scheduler tick → lance batch ou commande, bind contexte explicite
- Reconciliation
- Settlement
- Analytics recompute

---

## Rôle : Public (anonymous)

**Prérequis :**

1. Request Context construit — scope `PUBLIC`
2. Tenant public par défaut (`tchalanet`)
3. JWT optionnel → préférer tenant JWT si présent
4. Pas d'Operational Context

**Actions disponibles :**
- Vérification ticket public → voir [verify-ticket](../../../02-functional/flows/verify-ticket.md)
- Résultats tirage public → voir [draw-execution](../../../02-functional/flows/draw-execution.md)
- Page model public

---

## Matrice de synthèse

| Rôle | Request Context | Operational Context | Actions clés |
|---|---|---|---|
| Seller POS | `TENANT` | Requis (trusted) | Sell, payout, offline |
| Tenant Admin | `ADMIN` | Non requis | Gestion users/outlets/settings |
| Admin POS | `ADMIN` | Requis (ADMIN_SELECTION) | Sell, payout via sélection explicite |
| Super-admin | `SUPER_ADMIN` | Non requis (sauf sélection) | Platform ops, override tenant |
| System/Batch | `SYSTEM` ou `TENANT` | Non requis | Batch, scheduler, replay |
| Public | `PUBLIC` | Non requis | Verify ticket, draw results |
