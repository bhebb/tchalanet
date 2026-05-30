# Operational Context — Contexte opérationnel POS/terrain

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — `common.context.operational`, `platform.operationalcontext`, `core.*`  
> **Remplace** : `docs/architecture/OPERATIONAL_CONTEXT.md` · `docs/conventions/user-contexte-operational.md` (sections validation et owner)  
> **Dernière mise à jour** : 2026-05-30

---

## Définition

L'Operational Context répond à :

> **Cet acteur peut-il opérer maintenant sur ce terminal, cet outlet et cette session ?**

Il est **distinct** du Request Context :

| Concept | Question | Scope |
|---|---|---|
| Request Context | Qui appelle ? Quel tenant ? Quel scope ? | Universel — tous rôles |
| Operational Context | Cet acteur peut-il opérer sur ce terminal/outlet/session ? | POS/terrain uniquement |

---

## Quand est-il requis ?

**Requis** pour les actions sensibles POS :
- `sell` — vente de ticket
- `payout` — paiement terrain
- `offline grant` — autorisation hors ligne
- `offline sync` — synchronisation hors ligne
- Toute action POS sensible explicitement documentée

**Pas requis** pour :
- Dashboard admin
- Tenant settings
- Platform ops
- Vérification publique de ticket
- Batch reconciliation
- Reporting normal

---

## Structure

```java
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {}
```

---

## Sources de confiance

| Source | Niveau de confiance | Usage |
|---|---|---|
| `SERVER_BOOTSTRAP` | Fiable | Bootstrap serveur explicite |
| `SIGNED_DEVICE_BINDING` | Fiable | Terminal bindé avec signature device |
| `ADMIN_SELECTION` | Fiable | Admin en mode POS sélection explicite |
| `CLIENT_CLAIM` | **Non fiable** pour action sensible | Claim client non validé |
| `NONE` | **Non fiable** | Aucune source |

Pour une action sensible, toujours utiliser :

```java
ctx.trustedOperationalContextRequired()
ctx.trustedPosOperationalContextRequired()
ctx.sellerOperationalContextRequired()
ctx.adminOperationalContextRequired()
ctx.superAdminOverrideRequired()
```

---

## Règle fondamentale : attaché tôt, validé tard

```
Operational request context → attaché tôt (TchContextFilter / OperationalContextResolver)
Operational context         → validé tard, par action
```

L'`OperationalContextResolver` attache les informations opérationnelles depuis les headers.  
Il ne fait **pas** la validation métier lourde.

La validation lourde est déléguée à chaque domaine au moment de l'action.

---

## Validation late — exemple pour `sell`

```
1. Request Context existe
2. Permission SELL exprimée par la sécurité web
3. trusted operational context requis
4. terminal existe / appartient au tenant / non bloqué
5. outlet existe / appartient au tenant / actif
6. session existe / appartient au tenant
7. session terminal/outlet/seller match
8. session ouverte
9. gates action : cutoff, pricing, limits, promotions, idempotency
```

---

## Permission vs validation opérationnelle

Deux niveaux distincts :

**Permission** (exprimée au niveau web/security) :
> Cet acteur a-t-il **le droit** de tenter cette action ?  
> Ex : `ticket.sell`, `payout.execute`, `terminal.bind`, `session.open`

**Validation opérationnelle** (déléguée aux domaines) :
> Même avec la permission, peut-il faire cette action **ici et maintenant** avec ce terminal/outlet/session ?  
> Ex : seller avec permission SELL refusé si terminal bloqué, session fermée, mismatch terminal/outlet, seller non assigné, cutoff dépassé.

---

## Owner boundaries

Chaque domaine valide ce qu'il possède. Ne pas créer un "mega context service" qui décide de tout.

```
core.terminal           → terminal, binding, device trust, statut terminal
core.outlet             → outlet, statut, assignation
core.session            → session ouverte, match seller/session/terminal/outlet
core.sales              → sell/cancel/offline acceptance gates
core.payout             → payout execution gates
core.offlinesync        → grant/sync technical validation
platform.accesscontrol  → permission checks
```

---

## Admin POS — sélection explicite

Un admin ne devient pas automatiquement un seller.

```http
POST   /tenant/me/operational-context/select   ← sélection POS explicite
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

Source : `ADMIN_SELECTION`  
Les handlers valident ensuite les mêmes invariants terminal/outlet/session que pour un Seller.

---

## Matrice rôle × opération

| Opération | Seller | Admin | Super-admin | System |
|---|---|---|---|---|
| Vendre ticket | POS context requis | Admin POS sélection explicite requise | Override + POS context | Non autorisé |
| Payer | POS context requis | Admin POS + permission | Override + POS + permission | Flux système contrôlé |
| Offline grant | Politique domaine | Permission + politique explicite | Override + permission | Flux contrôlé uniquement |
| Offline sync replay | Seller original | Non (pas acteur) | Non (pas acteur) | Autorisé — seller original préservé |
| Admin tenant | Non autorisé | Permission requise | Override + permission | Non autorisé |

---

## Fail-fast order

```
1. trusted operational context
2. terminal existe / tenant
3. terminal locked / blocked / seller assignment
4. outlet existe / tenant
5. outlet status / blocked flags
6. session existe / tenant
7. session terminal/outlet/seller match
8. session status
9. action-specific gates
```

---

## Règle RLS

L'Operational Context ne bypass pas le RLS.

- Travail tenant-scoped → bind tenant dans `TchRequestContext`
- Override super-admin → explicite et auditable
- Scope `SYSTEM` → n'implique pas d'accès cross-tenant
