# Design — refactor-common-context-operational-roles

## Current State

Le backend possède déjà plusieurs briques correctes:

- `TenantContextLookup` existe dans `common.context.tenant`;
- `TenantConfigContextLookup` existe dans `platform.tenantconfig.internal.context`;
- `TchContextFilter` vit dans `common.context.web`;
- `TrustLevel` existe déjà avec les valeurs `NONE`, `WEAK`, `STRONG`;
- `OperationalRequestContext` existe déjà comme record plat;
- plusieurs validators `core` revalident déjà terminal/outlet/session au moment de l'action.

Ce change ne repart donc pas de zéro. Il précise les frontières et donne une trajectoire de migration.

## Target Package Organization

```text
common/context/
  TchContext.java
  TchContextHolder.java
  TchContextResolver.java
  TchContextScope.java
  TchRequestContext.java
  TchContextProperties.java
  TenantContextInfo.java
  ApiScope.java

  web/
    TchContextFilter.java
    CurrentContext.java
    CurrentContextArgumentResolver.java
    CurrentContextWebMvcConfig.java
    ApiScopeResolver.java
    TchRequestContextFactory.java
    OperationalContextHeaderParser.java

  tenant/
    TenantContextLookup.java
    TenantContextResolver.java

  system/
    SystemContextProperties.java
    SystemContextConfig.java
    SystemContextFactory.java

  operational/
    OperationalContextRole.java
    OperationalContextSource.java
    TrustLevel.java
    OperationalContextHint.java
    OperationalRequestContext.java
    PosOperationalContext.java
    SellerOperationalContext.java
    AdminOperationalContext.java
    SuperAdminOperationalContext.java
    OperationalContextHeaders.java
    MissingOperationalContextException.java
    UntrustedOperationalContextException.java
```

`common.context.operational` est un package de types neutres. Il peut parser et normaliser des entrées déjà présentes dans la requête, mais il ne peut pas appeler de repository, `CommandBus`, `QueryBus`, API platform, API core, API catalog ou feature.

## Context Model

Le modèle recommandé est une composition légère.

`TchRequestContext` reste le porteur du contexte runtime global:

- request id;
- correlation id;
- API scope;
- tenant effectif;
- actor/current user id;
- autorités/roles bruts;
- locale/timezone;
- mode system;
- override super-admin actif;
- contexte POS opérationnel optionnel.

Le frame POS porte les informations transactionnelles attachées tôt:

```java
public record PosOperationalContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,
    OperationalContextRole role,
    OperationalContextSource source,
    TrustLevel trustLevel
) {}
```

`sellerUserId` est volontairement distinct de `actorUserId`.

Pour une vente POS online, `sellerUserId == actorUserId` dans le cas nominal. Pour un offline replay, l'acteur courant peut être `SYSTEM` ou un service de sync, alors que `sellerUserId` représente le cashier qui a produit la vente d'origine.

## Role Semantics

### Seller

Seller POS représente un vendeur/cashier agissant dans un frame terminal/outlet/session.

Un seller context peut être `STRONG` si sa source est dérivée d'un binding signé, d'un bootstrap serveur ou d'une sélection validée côté serveur. Il peut être `WEAK` si la source est seulement un client claim.

### Admin

Un tenant admin n'est pas automatiquement vendeur.

En mode gestion, l'admin n'a pas besoin de POS frame. Pour agir sur une opération POS sensible, il doit explicitement sélectionner un frame opérationnel admin. Ce change spécifie les endpoints et le contrat de lecture, mais ne les implémente pas encore.

Endpoints spec-only:

```text
POST   /tenant/me/operational-context/select
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

La sélection active peut produire un frame avec `role = ADMIN`, `source = ADMIN_SELECTION`, `trustLevel = STRONG`. Le handler cible reste responsable de vérifier permissions et validité terminal/outlet/session.

### Super Admin

Un super-admin travaille normalement en scope platform sans tenant effectif.

Lorsqu'il agit dans un tenant, l'override est par requête:

```text
X-Tch-Tenant-Override: <tenantId-or-code>
X-Tch-Override-Reason: <reason>
```

L'override:

- requiert une permission dédiée `platform.tenant.override`;
- doit être auditable;
- ne dure pas au-delà de la requête;
- ne remplace aucune validation métier;
- n'implique pas un POS frame par défaut.

Si un super-admin doit aussi agir sur un POS, il doit avoir un POS frame explicite et les validators du domaine doivent appliquer les mêmes invariants qu'à tout autre acteur autorisé.

## Trust Levels

Les valeurs restent celles du code courant:

```java
public enum TrustLevel {
  NONE,
  WEAK,
  STRONG
}
```

`STRONG` signifie que la source du contexte est fiable pour commencer une opération sensible. Cela ne signifie jamais que le terminal existe, que la session est ouverte ou que le vendeur est autorisé. Ces validations restent tardives.

Sources recommandées:

| Source | Trust default | Notes |
| --- | --- | --- |
| `NONE` | `NONE` | Aucun contexte opérationnel. |
| `CLIENT_CLAIM` | `WEAK` | Headers client non signés; accepté seulement par des cas d'usage explicitement permissifs. |
| `SERVER_BOOTSTRAP` | `STRONG` | Contexte produit par le serveur. |
| `SIGNED_DEVICE_BINDING` | `STRONG` | Binding signé/validé, sans preuve métier terminal/session. |
| `ADMIN_SELECTION` | `STRONG` | Sélection admin active côté serveur. |
| `SUPER_ADMIN_OVERRIDE` | `STRONG` | Override tenant explicite et autorisé. |

## HTTP Pipeline

Pipeline cible:

```text
BearerTokenAuthenticationFilter
  -> UserBootstrapFilter
  -> TchContextFilter
      -> ApiScopeResolver
      -> TchRequestContextFactory
      -> TenantContextResolver
      -> Actor/user facts extraction
      -> OperationalContextHeaderParser
      -> operational attach without DB validation
      -> TchContextBinder.bind(finalCtx)
      -> finally clear holder, request attribute, MDC
```

`TchContextFilter` reste le producteur HTTP canonique. Aucun `OperationalContextFilter` séparé n'est autorisé.

## Header Parser And Attach

Le parser commun est pur:

- lit les headers HTTP;
- normalise les blancs;
- parse les typed ids quand le type est disponible;
- produit un hint ou un contexte opérationnel faible;
- ne fait aucun lookup DB;
- ne valide aucune ressource transactionnelle.

Headers opérationnels:

```text
X-Tch-Terminal-Id
X-Tch-Outlet-Id
X-Tch-Sales-Session-Id
X-Tch-Operational-Source
X-Tch-Tenant-Override
X-Tch-Override-Reason
```

Les headers sont des inputs, jamais une preuve. Un contexte dérivé seulement des headers client doit être `WEAK` sauf cas signé ou sélection serveur.

## Tenant Lookup Boundary

`TenantContextLookup` reste dans `common.context.tenant`, car `common.context` doit pouvoir dépendre d'une interface sans dépendre de platform.

`platform.tenantconfig.internal.context.TenantConfigContextLookup` fournit l'implémentation réelle.

Le lookup résout un tenant effectif à partir d'une valeur déjà extraite:

- tenant id;
- tenant code;
- claim JWT déjà lu;
- header override déjà autorisé par la politique de scope.

Il ne place pas de logique de routing métier complexe dans `common`. La résolution à partir de sous-domaines, routes publiques ou règles produit reste une responsabilité de platform/edge/web selon le cas.

## Validation Placement

### In Common

Autorisé:

- créer le contexte runtime;
- lire headers;
- parser typed ids;
- attacher un contexte opérationnel optionnel;
- marquer `NONE`, `WEAK` ou `STRONG`;
- fournir des helpers de présence/trust/role.

Interdit:

- query terminal/outlet/session;
- query permissions persistées;
- vérifier seller assignment;
- vérifier statut session;
- vérifier payout eligibility;
- vérifier offline sync eligibility;
- appeler `CommandBus` ou `QueryBus`;
- importer platform/core/catalog/features.

### In Core

Les domaines propriétaires valident les ressources au moment d'exécuter l'action.

Ordre recommandé pour les opérations POS sensibles:

1. contexte POS présent et suffisamment trusted;
2. terminal existe et appartient au tenant;
3. terminal actif, non locked, non blocked;
4. outlet existe et appartient au tenant;
5. outlet actif, non blocked;
6. session existe et appartient au tenant;
7. session correspond au terminal/outlet/seller;
8. session status compatible;
9. gates spécifiques à l'action.

Cette orchestration reste dans les validators/callers de `core.sales`, `core.payout`, `core.offlinesync`, `core.session`, `core.terminal` et `core.outlet` en V1.

## No New Operational Bounded Context In V1

Ce change ne crée pas:

- `core.operationalcontext`;
- `platform.operationalcontext`.

La duplication contrôlée des validations terminal/outlet/session est acceptée en V1 parce que les variations par cas d'usage existent réellement. Si la duplication devient problématique après migration, un change séparé pourra proposer une extraction.

Règle V1: les handlers/validators gardent la trace explicite des queries qu'ils utilisent, ce qui garde les erreurs et invariants lisibles près du use case.

## Operation Role Matrix

| Operation | SELLER | ADMIN | SUPER_ADMIN | SYSTEM |
| --- | --- | --- | --- | --- |
| Sell ticket online POS | Allowed with POS context | Only with explicit admin POS selection | Only with explicit override and explicit POS context | Not allowed |
| Cancel/void ticket POS | Allowed with POS context | Only with explicit admin POS selection and permission | Only with explicit override, permission and POS context | Not allowed |
| Payout POS | Allowed when domain permits | Only with explicit admin POS selection and permission | Only with explicit override, permission and POS context | Not allowed |
| Offline grant | Allowed when domain permits | Allowed with permission and explicit policy | Allowed with override and permission | Allowed only for system-issued flows |
| Offline sync replay | Not the actor in replay | Not usually the actor in replay | Not usually the actor in replay | Allowed as actor while preserving original sellerUserId |
| Terminal open/close | Allowed if assigned and permitted | Allowed with explicit admin POS selection and permission | Allowed with override, permission and POS context | Allowed for controlled maintenance flows only |
| Session open/close | Allowed if assigned and permitted | Allowed with explicit admin POS selection and permission | Allowed with override, permission and POS context | Allowed for controlled maintenance flows only |
| Tenant admin management | Not allowed | Allowed by permission | Allowed with override and permission | Not allowed |
| Platform tenant override | Not allowed | Not allowed | Allowed with `platform.tenant.override` | Not allowed |

The matrix is normative. Domains may add stricter rules but must not loosen these defaults without a new OpenSpec change.

## Admin POS Selection Contract

V1 specifies the read side only. The write endpoints are not implemented by this change.

Contract shape:

```java
public interface AdminPosSelectionLookup {
  Optional<AdminPosSelection> findActive(TenantId tenantId, UserId userId);
}

public record AdminPosSelection(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    Instant expiresAt
) {}
```

The V1 implementation may be a stub returning `Optional.empty()`. This lets the filter and context model evolve without pretending the admin POS selection lifecycle is already implemented.

## TchRequestContext Helpers

Helpers should be minimal and typed.

Recommended helpers:

```java
PosOperationalContext posOperationalContextRequired();
PosOperationalContext trustedPosOperationalContextRequired();
SellerOperationalContext sellerOperationalContextRequired();
AdminOperationalContext adminOperationalContextRequired();
SuperAdminOperationalContext superAdminOverrideRequired();
```

The bridge may keep existing flat helpers during migration, but new sensitive handlers should prefer role-aware helpers.

## Bridge Migration

The existing flat `OperationalRequestContext` remains as a temporary bridge.

Migration requirements:

1. mark the flat bridge deprecated once the typed replacement exists;
2. prevent new usages outside the bridge package with ArchUnit or equivalent tests;
3. migrate domains in this order:
   - `core.sales`;
   - `core.payout`;
   - `core.offlinesync`;
   - `core.session`;
   - remaining POS/terminal flows;
4. remove the bridge only after all call sites and tests have moved.

## Test Strategy

Parser tests:

- no operational headers returns no context;
- client headers produce `CLIENT_CLAIM` and `WEAK`;
- signed/server/admin selection sources produce `STRONG`;
- blank headers are normalized away;
- malformed ids fail as parsing errors, not DB validation errors.

Context helper tests:

- missing context;
- untrusted context;
- wrong role;
- seller POS required;
- admin POS required;
- super-admin override required.

ArchUnit tests:

- `common.context.operational` does not import repositories;
- `common.context.operational` does not import `CommandBus` or `QueryBus`;
- `common.context` does not import platform/core/catalog/features;
- no class named `OperationalContextFilter`;
- no package named `platform.usercontext`;
- platform tenantconfig may implement `TenantContextLookup`.

OpenSpec validation:

```bash
pnpm exec openspec validate refactor-common-context-operational-roles
```
