# Proposal — refactor-common-context-operational-roles

## Status

PROPOSED

## Why

`common.context` doit rester le modèle runtime neutre de la requête courante, mais le contexte opérationnel actuel est trop plat et ambigu.

Aujourd'hui, `OperationalRequestContext` porte principalement `terminalId`, `outletId`, `salesSessionId`, `source` et `trustLevel`. Cette forme suffit pour les validations POS simples, mais elle ne dit pas clairement:

- si l'acteur courant est un vendeur, un admin tenant, un super-admin ou un traitement system;
- si un admin agit en mode gestion ou a explicitement sélectionné un frame POS;
- si un super-admin a fait un override tenant explicite et auditable;
- si le vendeur opérationnel d'une vente offline rejouée est différent de l'acteur courant qui exécute la synchronisation;
- où placer la frontière entre parsing HTTP, contexte runtime et validation métier terminal/outlet/session.

Le refactoring `common / catalog / platform / core / features` a déjà fixé les dépendances de modules. Ce change stabilise maintenant `common.context` sans déplacer les règles métier vers `common` et sans créer de nouveau bounded context transversal en V1.

## What Changes

Ce change définit une migration de `common.context` autour de cinq axes.

1. **Organisation des packages runtime**

   `common.context` est structuré explicitement:

   - `common.context`
   - `common.context.web`
   - `common.context.tenant`
   - `common.context.system`
   - `common.context.operational`

   `TchContextFilter` reste dans `common.context.web` et demeure le producteur HTTP canonique. Aucun `OperationalContextFilter` séparé ne doit être créé.

2. **Contexte opérationnel typé mais neutre**

   `common.context.operational` contient seulement des types runtime neutres: rôles, sources, trust level, headers, hints/parser purs, exceptions et records de contexte opérationnel. Il ne contient aucune règle métier et ne fait aucun lookup DB.

   Le modèle cible conserve `actorUserId`, scope, tenant effectif, autorités et override super-admin dans `TchRequestContext`, puis attache un frame POS opérationnel lorsque la requête en transporte un. Le `sellerUserId` du frame POS peut être différent de `actorUserId`, notamment pour le replay offline.

3. **Attach early, validate late**

   Le filtre peut lire les headers et attacher un contexte opérationnel brut/typé avec un `TrustLevel`.

   Il ne valide jamais:

   - existence de terminal/outlet/session;
   - appartenance au tenant;
   - statut actif/bloqué/verrouillé;
   - session ouverte;
   - cohérence terminal/outlet/session/seller;
   - permissions métier d'un payout, d'une vente, d'un offline sync ou d'une opération POS.

   Les handlers et validators des domaines propriétaires restent responsables des validations transactionnelles.

4. **Rôles opérationnels et super-admin override**

   Le change formalise les rôles opérationnels:

   - seller POS;
   - admin tenant en mode gestion;
   - admin tenant en mode POS explicite;
   - super-admin avec override tenant par requête;
   - system/batch.

   Le super-admin override est explicite, par requête, auditable, et utilise:

   - `X-Tch-Tenant-Override`
   - `X-Tch-Override-Reason`

   La permission dédiée à spécifier est `platform.tenant.override`.

5. **Compatibilité et migration**

   Le record plat actuel `OperationalRequestContext` reste disponible comme bridge temporaire. Le change impose toutefois un plan de migration, une dépréciation contrôlée et des règles ArchUnit pour éviter que ce bridge devienne permanent.

## Non-Goals

Ce change ne doit pas:

- créer `platform.operationalcontext`;
- créer `core.operationalcontext`;
- créer `platform.usercontext`;
- créer un `OperationalContextFilter`;
- déplacer les validations terminal/outlet/session dans `common`;
- déplacer les permissions persistées dans `common`;
- implémenter tout de suite les endpoints admin POS;
- remplacer les validators métier existants des domaines `core.sales`, `core.payout`, `core.offlinesync`, `core.session`, `core.terminal` ou `core.outlet`.

## Impact

### Common

- Réorganisation des packages `common.context`.
- Ajout des types neutres de `common.context.operational`.
- Ajout d'un parser pur des headers opérationnels.
- Ajout de helpers typés dans `TchRequestContext`.
- Maintien d'un bridge compatible pour le contexte opérationnel plat.

### Platform

- Confirmation que `TenantContextLookup` reste dans `common.context.tenant`.
- Confirmation que `platform.tenantconfig.internal.context.TenantConfigContextLookup` implémente ce lookup.
- Spécification du contrat de lecture admin POS en V1 avec stub/no-op possible.
- Spécification du super-admin override et de l'audit associé.

### Core

- Les domaines conservent l'orchestration terminal/outlet/session en V1.
- Les validations restent proches des use cases sensibles.
- Aucune extraction vers un bounded context `core.operationalcontext`.
- Migration progressive des handlers vers les helpers typés.

### Docs And Tests

- Mise à jour des conventions de contexte runtime et operational context.
- Ajout de tests parser/helpers.
- Ajout de règles ArchUnit sur les dépendances interdites et les filtres interdits.
- Validation OpenSpec requise:

```bash
pnpm exec openspec validate refactor-common-context-operational-roles
```
