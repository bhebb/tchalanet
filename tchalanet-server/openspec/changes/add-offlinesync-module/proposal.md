# Add offlinesync module — Proposal v2.1

## Why

Des vendeurs Tchalanet peuvent perdre la connectivité réseau pendant une période de vente. Le système doit permettre une vente offline bornée, traçable, cryptographiquement signée, puis réconciliée sans double ticket et sans ouvrir une faille de fraude.

## What Changes

- Ajout du module `core.offlinesync`.
- Ajout des entités `OfflineGrant`, `OfflineCodeBatch`, `OfflineCode`, `OfflineSyncBatch`, `OfflineSubmission`, `OfflineSubmissionLine`, `OfflineSubmissionTicketLink`, `OfflineSubmissionDecision`.
- Ajout d’events self-contained dans `core.offlinesync.api.event`.
- Ajout d’un listener dans `core.sales` pour promouvoir les submissions.
- Ajout de `ticket.offline_submission_id` avec contrainte unique `(tenant_id, offline_submission_id)`.
- Ajout de `GetOfflineLimitPolicyQuery` dans `core.limitpolicy`.
- Ajout des endpoints POS, admin et ops pour offline.
- Ajout du flow Flutter offline.

## Correctifs v2.1

- `validUntil` et `syncAcceptedUntil` séparés.
- Aucun retour `RESERVED -> AVAILABLE` pour un code soumis.
- `DUPLICATE` résultat API seulement.
- Events dans `api.event`, pas `internal`.
- Events vers `sales` self-contained.
- Retour obsolète ignoré par `promotionAttemptId`.
- Contexte opérationnel trusted obligatoire pour grant et sync.
- Jobs thin et commands dédiées.

## Impact

### New

- `core.offlinesync`
- tables `offline_*`
- endpoints `/tenant/offline/**`, `/admin/offline/**`, `/platform/ops/offline/**`

### Modified

- `core.sales`
- `core.limitpolicy`
- Flutter Android POS

## Out of scope

- Auto-risk scoring avancé.
- QR public de vérification offline-to-offline.
- Outbox Kafka externe.
