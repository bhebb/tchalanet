# Operational Context — Contexte opérationnel POS/terrain

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — `common.context.operational`, `platform.accesscontrol`, `core.*`  
> **Dernière mise à jour** : 2026-06-20

---

## Définition

L'Operational Context répond à :

> **Cet acteur peut-il opérer maintenant ?**

Il est **distinct** du Request Context :

| Concept | Question | Scope |
|---|---|---|
| Request Context | Qui appelle ? Quel tenant ? Quel actorType ? | Universel — tous acteurs |
| Operational Context | Cet acteur peut-il opérer ? | POS/terrain — SellerTerminal + Admin POS |

---

## Modèle SellerTerminal (actuel)

Pour un acteur `SELLER_TERMINAL`, l'Operational Context est intrinsèque à l'acteur lui-même :

```
TchRequestContext.actorType = SELLER_TERMINAL
TchRequestContext.sellerTerminalId = <UUID>
```

**Il n'y a pas de Terminal/Outlet/Session séparés à résoudre.**

Les validations lors d'une action sensible sont :

```
1. actorType = SELLER_TERMINAL
2. sellerTerminalId non null
3. seller_terminal.status = ACTIVE (BLOCKED → 403, DISABLED → 403)
4. seller_terminal.mustChangePin = false (true → 403 avec requiredStep MUST_CHANGE_PIN)
5. permission accordée (ex: ticket.sell)
6. gates action-specific (cutoff, pricing, limits, promotions, idempotency)
```

---

## Admin POS — sélection explicite

Un admin `APP_USER` ne devient pas automatiquement un SellerTerminal.

Il sélectionne explicitement un SellerTerminal pour accéder aux fonctions POS :

```http
POST /tenant/cashier/operational-context/select
GET  /tenant/cashier/operational-context
DELETE /tenant/cashier/operational-context
```

Source : `ADMIN_SELECTION`  
Les handlers valident ensuite les mêmes invariants que pour un SellerTerminal.

---

## Quand l'Operational Context est-il requis ?

**Requis** pour les actions sensibles POS :
- `sell` — vente de ticket
- `payout` — paiement terrain
- `offline grant` — autorisation hors ligne
- `offline sync` — synchronisation hors ligne

**Pas requis** pour :
- Dashboard admin
- Tenant settings
- Platform ops
- Vérification publique de ticket
- Batch reconciliation
- Reporting normal

---

## Permission vs validation opérationnelle

**Permission** (exprimée au niveau web/security) :
> Cet acteur a-t-il **le droit** de tenter cette action ?  
> Ex : `ticket.sell`, `payout.execute`

**Validation opérationnelle** (déléguée aux domaines) :
> Même avec la permission, peut-il faire cette action **ici et maintenant** ?  
> Ex : SellerTerminal avec permission SELL refusé si `BLOCKED`, `mustChangePin=true`, cutoff dépassé.

---

## Owner boundaries

Chaque domaine valide ce qu'il possède :

```
core.sellerterminal     → statut, mustChangePin, identité
core.sales              → sell/cancel gates, cutoff, limits
core.payout             → payout execution gates
core.offlinesync        → grant/sync technical validation
platform.accesscontrol  → permission checks
```

---

## Fail-fast order (SellerTerminal)

```
1. actorType = SELLER_TERMINAL
2. sellerTerminalId non null
3. seller_terminal existe / tenant OK
4. seller_terminal.status = ACTIVE (non bloqué)
5. seller_terminal.mustChangePin = false
6. permission accordée
7. gates action-specific
```

---

## Fail-fast order (Admin POS)

```
1. actorType = APP_USER
2. rôle TENANT_ADMIN (ou permission explicite)
3. ADMIN_SELECTION effectuée (sellerTerminalId présent dans le contexte session)
4. SellerTerminal sélectionné : ACTIVE, non bloqué
5. permission accordée
6. gates action-specific
```

---

## Règle RLS

L'Operational Context ne bypass pas le RLS.

- Travail tenant-scoped → bind tenant dans `TchRequestContext`
- Override super-admin → explicite et auditable
- Scope `SYSTEM` → n'implique pas d'accès cross-tenant

---

## Matrice acteur × opération

| Opération | SellerTerminal | Admin POS | Super-admin | System |
|---|---|---|---|---|
| Vendre ticket | Autorisé si ACTIVE + perm | Sélection explicite + perm | Override + ADMIN_SELECTION | Non autorisé |
| Payer | Autorisé si ACTIVE + perm | ADMIN_SELECTION + perm | Override + perm | Flux contrôlé uniquement |
| Offline grant | Politique domaine | Perm + politique explicite | Override + perm | Flux contrôlé uniquement |
| Admin tenant | Non autorisé | Perm requise | Override + perm | Non autorisé |

---

## Note sur l'ancien modèle (retrait)

L'`OperationalRequestContext` avec `TerminalId`, `OutletId`, `SalesSessionId` est retiré du modèle actif.  
Les tables `terminal`, `sales_session` restent en DB pour l'historique mais ne sont plus alimentées par de nouvelles ventes.
