# Spec Delta — TenantTheme (core/tenanttheme)

## Objectif

Introduire le module `core/tenanttheme` responsable de l'application et du cycle de vie des ThemePresets au niveau tenant. Ce module consomme `catalog/theme` (lecture) et gère l'assignation, la version, l'audit et la publication d'événements tenant-scoped.

## ADDED Requirements

### Requirement: T1 — Apply tenant theme

Le système MUST permettre l'application d'un ThemePreset pour un tenant via une commande `ApplyTenantThemeCommand`.

- Command handler: `core/tenanttheme/application/command/handler/ApplyTenantThemeCommandHandler` MUST valider l'existence et la disponibilité du preset via `ThemePresetCatalog` avant de persister.
- Le handler MUST persister une entité `TenantTheme` tenant-scoped (table `tenant_theme`) et incrémenter la version.
- Après commit, le handler MUST publier un événement `TenantThemeUpdatedEvent`.

#### Scenario: appliquer un preset valide

- Given : un tenant T et un preset code `dark-v1` actif dans `catalog/theme`
- When : `ApplyTenantThemeCommand(tenantId=T, presetCode=dark-v1)` est envoyé
- Then : `tenant_theme` contient une ligne associant T ↔ preset, version incrémentée, et `TenantThemeUpdatedEvent` est publié

#### Scenario: preset invalide

- Given : preset `archived-theme` est soft-deleted dans `catalog/theme`
- When : `ApplyTenantThemeCommand` pointe vers `archived-theme`
- Then : le handler MUST rejeter la commande avec une erreur lisible (ex: NotFound/BadRequest) sans persister

---

### Requirement: T2 — TenantTheme persistence & RLS

La persistance des associations tenant → preset MUST être tenant-scoped et respecter la politique RLS (Row-Level Security) appropriée.

- Table attendue : `tenant_theme` (tenant_id, theme_preset_id, metadata JSONB, version, created_at, updated_at, created_by)
- Les accès en lecture/écriture MUST être contrôlés par RLS policies côté base.

#### Scenario: persistance tenant-scoped

- Given : Postgres avec RLS activé pour `tenant_theme`
- When : `ApplyTenantThemeCommand` écrit pour tenant T
- Then : la ligne est insérée et visible uniquement aux sessions autorisées pour T

---

### Requirement: T3 — Validation & consistency

Avant toute écriture, le module MUST valider que :

- le preset référencé existe et est actif dans `ThemePresetCatalog` ;
- la commande contient un tenantId valide.

#### Scenario: validation cross-module

- Given : catalogue contient `modern-light`; tenantId est valide
- When : handler valide la commande
- Then : la persistance continue

---

### Requirement: T4 — Events & idempotence

Les handlers MUST publier `TenantThemeUpdatedEvent` après commit et garantir l'idempotence des commandes (réessai sans duplication de state).

- Event payload : tenantId, themePresetId, version, timestamp, initiator

#### Scenario: commande réessayée

- Given : ApplyTenantThemeCommand a été traité partiellement (timeout avant ack)
- When : même commande est renvoyée
- Then : le handler doit être idempotent et produire le même état sans doublon d'événement final (ou publish de façon sûre)

---

### Requirement: T5 — API porteuse (ports & handlers)

Le module MUST exposer :

- ports/out pour `TenantThemePersistencePort` (utilisé par adapters infra)
- un contrat de commande `ApplyTenantThemeCommand` et `DeactivateTenantThemeCommand` avec handlers

#### Scenario: port abstraction

- Given : impl infra pour Postgres
- When : handler appelle `TenantThemePersistencePort.save`
- Then : l'adapter persiste selon la politique RLS

---

## Non-Fonctionnelles (NF)

### NF1 — Sécurité

- Les commandes de modification MUST être restreintes aux rôles autorisés (platform/tenant-admin selon contexte).
- Toutes les opérations impliquant tenantId MUST respecter le contexte d'autorisation et audit.

### NF2 — Observabilité

- Les handlers MUST journaliser les changements critiques et émettre métriques (counter pour apply, durations).
- Les événements MUST être traçables via corrélation id (traceId / commandId).

### NF3 — Résilience

- Handlers MUST être transactionnels et retenter de façon sûre en cas d'erreurs transitoires.

---

## Critères d'acceptation

- Tests unitaires et d'intégration (H2 ou Testcontainers) validant Apply/Deactivate flows passent.
- Tests E2E démontrant que `core/tenanttheme` lit `catalog/theme` et refuse les presets soft-deleted.
- Idempotence démontrée via tests (réessai d'une même commande ne crée pas doublons).
- `tenant_theme` table migration script ready (défini dans tasks.md) — migration deferred until implementation.

---

## Déploiement & migration (notes)

- Déployer `core/tenanttheme` après `catalog/theme` (catalog must be available to validate presets).
- Migration DB pour `tenant_theme` doit être réalisée avec scripts idempotent et tests.

---

<!-- EOF -->
