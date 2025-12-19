# Domaine Tenant — Tchalanet

## Table des matières

1. [Rôle du domaine Tenant](#r%C3%B4le-du-domaine-tenant)
2. [Responsabilités](#responsabilit%C3%A9s)
3. [Modèle métier (Domain Model)](#mod%C3%A8le-m%C3%A9tier-domain-model)
4. [Cycle de vie (State Machine)](#cycle-de-vie-state-machine)
5. [Persistance (table tenant)](#persistance-table-tenant)
6. [Ports & Adapters (Hexagonal)](#ports--adapters-hexagonal)
7. [Événements de domaine](#%C3%A9v%C3%A9nements-de-domaine)
8. [Cache](#cache)
9. [AppSettings & Tenant](#appsettings--tenant)
10. [API Web (Admin)](#api-web-admin)
11. [Ce que le domaine Tenant ne fera jamais](#ce-que-le-domaine-tenant-ne-fera-jamais)

---

## 1. Rôle du domaine Tenant

Le domaine Tenant représente un opérateur de la plateforme Tchalanet (ex : réseau de borlette, opérateur ambulant, tenant démo).

Il est la racine organisationnelle :

- propriétaire des outlets
- propriétaire des utilisateurs
- porteur des règles de configuration (via app_setting)
- unité de sécurité, facturation, reporting et isolation des données

> Un tenant n’exécute aucune logique métier de vente, mais oriente le comportement des autres domaines.

## 2. Responsabilités

Le domaine Tenant est responsable de :

- Identité et cycle de vie d’un tenant
- État global (actif / suspendu / archivé)
- Configuration globale (timezone, currency, branding)
- Émission d’événements structurants (TenantCreated, TenantArchived, …)
- Cache & résolution rapide (tenant by code)

Le domaine Tenant n’est pas responsable de :

- ventes (sales)
- sessions POS (session)
- paiements (payout)
- règles de mise (limitpolicy)
- reporting/statistiques (features)

## 3. Modèle métier (Domain Model)

**Aggregate Root : Tenant**

Exemple (pseudo-code) :

```java
public class Tenant {
    private final TenantId id;
    private final String code;
    private final String name;
    private final TenantType type;
    private final TenantStatus status;
    private final String timezone;
    private final String currency;

    // branding
    private final UUID activeThemeId;
    private final UUID addressId;

    // invariants:
    // - code unique
    // - timezone obligatoire
    // - currency ISO-4217
}
```

**Value Objects**

- TenantId (UUID)
- TenantType : BORLETTE, RESEAU, AMBULANT
- TenantStatus : ACTIVE, SUSPENDED, ARCHIVED

> Remarque : Status = lifecycle (fusion volontaire).

## 4. Cycle de vie (State Machine)

États et transitions :

```
CREATED
  ↓ activate
ACTIVE
  ↓ suspend
SUSPENDED
  ↓ activate
ACTIVE
  ↓ archive
ARCHIVED (final)
```

Règles :

- ARCHIVED est irréversible
- Aucun ticket / session / payout autorisé si status != ACTIVE
- Les transitions sont exclusivement déclenchées par des CommandHandlers

## 5. Persistance (table tenant)

Table finale (proposition SQL) :

```sql
CREATE TABLE tenant (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL,
  code            varchar(64) NOT NULL UNIQUE,
  name            varchar(255) NOT NULL,
  type            varchar(32) NOT NULL,
  status          varchar(32) NOT NULL,
  timezone        varchar(64) NOT NULL,
  currency        varchar(3)  NOT NULL,
  active_theme_id uuid,
  address_id      uuid,
  created_at      timestamptz NOT NULL,
  created_by      uuid,
  updated_at      timestamptz NOT NULL,
  updated_by      uuid,
  deleted_at      timestamptz
);
```

Notes :

- Pas de `tenant_config` table dédiée. Toute config dynamique → `app_setting`.
- `deleted_at` utilisé uniquement pour audit / soft delete technique.

## 6. Ports & Adapters (Hexagonal)

Ports IN (Application) :

- CreateTenantCommand
- ActivateTenantCommand
- SuspendTenantCommand
- ArchiveTenantCommand
- GetTenantByIdQuery
- GetTenantByCodeQuery

Ports OUT :

- TenantRepositoryPort
- TenantCachePort
- DomainEventPublisher

## 7. Événements de domaine

Émis par le domaine Tenant :

- TenantCreatedEvent
- TenantActivatedEvent
- TenantSuspendedEvent
- TenantArchivedEvent

> Ces events sont structurels :
>
> - déclenchent le seed `app_setting`
> - invalident les caches
> - permettent à d’autres domaines de réagir (billing, reporting)

## 8. Cache

Stratégie :

- clé critique : `tenant.code` → tenant
- cache combiné :
  - L1 (Caffeine, ~10 min)
  - L2 (Redis, ~30–60 min)

Éviction :

- sur `@PostPersist` / `@PostUpdate`
- via `TenantCacheEvictListener`
- jamais basé uniquement sur TTL

## 9. AppSettings & Tenant

Le domaine Tenant ne stocke pas directement ses règles métier.
Il délègue à `app_setting` pour :

- comportement POS
- horaires d’ouverture (tenant / outlet)
- règles de payout
- offline sync
- UI / i18n

> Le domaine Tenant garantit seulement :
>
> - qu’un tenant possède des settings valides
> - que les clés proviennent du `AppSettingRegistry`

## 10. API Web (Admin)

Base path : `/admin-api/tenants`

Exemples d'endpoints :

- `POST /admin-api/tenants`
- `POST /admin-api/tenants/{id}/activate`
- `POST /admin-api/tenants/{id}/suspend`
- `POST /admin-api/tenants/{id}/archive`

> Remarque : Les endpoints appellent CommandBus / QueryBus, jamais directement les repositories.

## 11. Ce que le domaine Tenant ne fera jamais

- ❌ Calculer des stats
- ❌ Gérer des sessions caissier
- ❌ Appliquer des règles de mise
- ❌ Gérer les paiements
- ❌ Porter des permissions utilisateur

---

_Document réorganisé pour lisibilité — contenu inchangé._
