# Add offlinesync module — Proposal

> **Version 2** — incorpore les retours de revue. Crypto Ed25519 device, séparation `validUntil` / `syncAcceptedUntil`, idempotence stricte avec `payloadHash`, events self-contained avec `promotionAttemptId`.

## Why

Les terminaux POS sont déployés dans des contextes opérationnels où la connectivité Internet n'est pas garantie : zones rurales, réseaux instables, pannes ponctuelles d'infrastructure. Aujourd'hui, une coupure réseau interrompt la vente, ce qui entraîne :

- **Perte de revenu** : ventes manquées pendant la durée de la panne
- **Insatisfaction client** : files d'attente, clients qui repartent
- **Insatisfaction vendeur** : impossibilité d'exercer son activité
- **Pression commerciale** : différenciation produit attendue par les tenants

Le module `core.offlinesync` répond à ce besoin en permettant aux POS de **continuer à vendre hors ligne** pendant une période bornée, avec des garanties cryptographiques fortes (signature Ed25519 par clé privée non exfiltrable du device), des quotas configurables, et une réconciliation propre lors du retour de la connectivité.

Le offline est positionné comme une **fonctionnalité premium** (plan BUSINESS et PREMIUM uniquement), créant un argument commercial pour la montée en gamme.

## What Changes

### Nouveau module Spring Modulith

- `core.offlinesync` avec ses sous-packages api/internal selon la convention projet
- 8 entités principales : `OfflineGrant`, `OfflineCodeBatch`, `OfflineCode`, `OfflineSyncBatch`, `OfflineSubmission`, `OfflineSubmissionLine`, `OfflineSubmissionTicketLink`, `OfflineSubmissionDecision`
- 7 commands principales
- 3 events Modulith **self-contained** avec `promotionAttemptId` pour idempotence stricte

### Sécurité crypto v2 — Ed25519 device

- Chaque device génère sa paire Ed25519 dans Android Keystore (clé privée non exfiltrable)
- Le serveur stocke uniquement la clé publique du device
- **Le serveur ne stocke aucun secret capable de forger des submissions**
- Les grants sont signés par le serveur avec sa propre clé privée Ed25519 (KMS-managed)

### Intégrations existantes

- `core.session` : consommé via `ResolvePosOperationContextQuery`
- `core.sales` : nouveau listener `@ApplicationModuleListener` qui consomme `OfflineSubmissionTechValidatedEvent` (self-contained) et publie `OfflineSubmissionProcessedEvent` avec `promotionAttemptId`
- `core.limitpolicy` : nouvelle query `GetOfflineLimitPolicyQuery`
- `platform.audit` : `@Audited` sur les nouvelles entités
- `platform.communication` : alertes admin

### Modification critique côté `core.sales`

Ajout d'une colonne `offline_submission_id` sur la table ticket avec contrainte UNIQUE `(tenant_id, offline_submission_id)`. **Garantit qu'il est physiquement impossible de créer deux tickets pour une même submission**, indépendamment des bugs logiciels potentiels du listener.

### Endpoints REST

Identiques à v1 (voir spec). Codes d'erreur étendus (voir DOMAIN_OFFLINESYNC.md §12).

### Frontend Flutter Android

- Génération keypair Ed25519 dans Android Keystore au premier lancement
- Service local de gestion du Grant (storage Drift + secure storage pour métadonnées)
- Service de vente offline (transaction locale atomique avec signature Ed25519)
- Service de sync (batch, retry, idempotence avec `batchPayloadHash` persisté avant appel HTTP)
- Service de renouvellement automatique du grant
- UI : indicateur mode offline, badge submissions en attente, mode offline forcé, réimpression DUPLICATA
- **9 statuts locaux explicites** pour tracer chaque submission de DRAFT à SYNCED/REJECTED

## Impact

### Specs affected

- **NEW** `offlinesync` — module entièrement nouveau

### Code affected

- **NEW** `core/offlinesync/` — module complet
- **MODIFIED** `core/sales/` — listener cross-module + colonne `offline_submission_id` avec UNIQUE constraint
- **MODIFIED** `core/limitpolicy/` — `GetOfflineLimitPolicyQuery` exposée
- **NEW** Migration Flyway : 8 nouvelles tables + 1 ALTER côté sales
- **NEW** Configuration KMS pour les clés serveur Ed25519
- **NEW** 3 jobs scheduled : `StuckSubmissionRecoveryJob`, `OrphanedCodeReservationJob`, `GrantExpirationJob`

### Frontend affected

- **NEW** Module Flutter `features/offline/` avec data/domain/presentation layers
- **NEW** Setup Ed25519 + Android Keystore via `cryptography` Dart package
- **MODIFIED** Flow de vente existant : routage online vs offline

### Out of scope (roadmap)

- iOS support (v2)
- Workflow de remboursement formalisé (v2)
- Rotation des clés serveur Ed25519 (v2)
- Module `core.risk` complet (v3)
- Vérification offline-to-offline avec QR signé (v4)
- Promotion asynchrone via broker (v4 si volume justifie)

## Risks

| Risque | Probabilité | Impact | Mitigation |
|---|---|---|---|
| Compromission backend | Faible | Limité | Ed25519 device : le backend ne peut PAS forger de submissions |
| Compromission device individuel | Moyenne | Local | Révocation grant, signatures liées au device compromis seulement |
| Rejet métier après ticket imprimé | Élevée | Moyen | TechnicalPolicy stricte au sync, workflow remboursement v2 |
| Double-création ticket suite à bug listener | Faible | Élevé | Contrainte UNIQUE côté sales + `promotionAttemptId` + listener idempotent |
| Abus du mode offline | Moyenne | Moyen | Visibilité v1, auto-restriction v3 (core.risk) |
| Désynchronisation horloge device | Moyenne | Faible | Tolérance 5min, NTP forcé v2 |
| Surcharge backend lors sync massif | Faible | Moyen | Batch max 50, throttling possible v2 |
| Conflit d'idempotence (bug client génère même UUID avec contenus différents) | Faible | Moyen | `payloadHash` + `batchPayloadHash`, erreurs explicites, alertes |
| Code "perdu" suite à crash serveur pendant validation | Faible | Faible | `OrphanedCodeReservationJob` récupère les RESERVED orphelins |
| Vente perdue par désinstall app avec sync pending | Faible | Élevé pour le client | UI bloquante au logout, dashboard admin grants expirés sans sync complète |
| Bug introduisant régression sécurité crypto | Très faible | Critique | Tests de propriétés cryptographiques, code review crypto obligatoire, audit externe avant prod |
