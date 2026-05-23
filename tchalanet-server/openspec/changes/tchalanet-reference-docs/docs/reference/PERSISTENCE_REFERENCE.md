# Tchalanet — Persistence Reference

> Status: NORMATIVE  
> Scope: PostgreSQL, Flyway, JPA, RLS, audit columns, idempotence, timestamps, locking  
> Owner: Backend / Data / Security

## 1. Objectif

La persistence Tchalanet doit garantir :

```text
- isolation stricte des tenants
- cohérence des transactions argent/ticket/session
- auditabilité
- idempotence
- résistance aux retries/concurrence
- migrations reproductibles
- lisibilité des modèles
```

Règle directrice :

```text
La base de données est le dernier rempart de sécurité.
Le code applicatif exprime les règles.
PostgreSQL les verrouille avec contraintes, transactions et RLS.
```

## 2. Principes non négociables

```text
1. Flyway only. ddl-auto=validate.
2. UUID brut uniquement en persistence/SQL.
3. Typed IDs hors persistence.
4. Tables tenant-scoped = tenant_id + RLS + indexes.
5. Timestamps business = Instant / timestamptz.
6. Pas de LocalDateTime pour moment métier.
7. Writes critiques = transactions explicites.
8. Contraintes DB pour empêcher les doubles effets.
9. Soft-delete seulement si le domaine le justifie.
10. Audit fonctionnel sur actions sensibles.
```

## 3. Typed IDs et persistence

Hors persistence :

```text
TenantId, UserId, TerminalId, TicketId, OutletId, SalesSessionId, PayoutId
```

En persistence :

```text
uuid
```

Mapper explicitement :

```java
UUID fromTerminalId(TerminalId id) {
  return id == null ? null : id.value();
}

TerminalId toTerminalId(UUID id) {
  return TerminalId.nullableOf(id);
}
```

Interdits :

```text
UUID dans command/query/event/domain model
Optional<UUID> hors persistence
UUID.fromString dans controller
```

## 4. Structure standard table tenant-scoped

```sql
create table terminal (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  outlet_id uuid null references outlet(id),
  code varchar(64) not null,
  label varchar(128) not null,
  type varchar(32) not null,
  status varchar(32) not null,
  capabilities jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  created_by uuid null,
  updated_at timestamptz not null default now(),
  updated_by uuid null,
  deleted_at timestamptz null,
  version bigint not null default 0,
  constraint uq_terminal_tenant_code unique (tenant_id, code),
  constraint ck_terminal_type check (type in ('PHYSICAL_POS','VIRTUAL_PHONE','VIRTUAL_WEB')),
  constraint ck_terminal_status check (status in ('PENDING_ACTIVATION','ACTIVE','LOCKED','REVOKED','EXPIRED'))
);

create index idx_terminal_tenant_status on terminal (tenant_id, status);
create index idx_terminal_tenant_outlet on terminal (tenant_id, outlet_id);
```

## 5. RLS standard

Chaque table tenant-scoped doit avoir :

```sql
alter table terminal enable row level security;

create policy terminal_tenant_isolation
on terminal
using (tenant_id = current_setting('app.tenant_id', true)::uuid)
with check (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

Règles :

```text
- Ne pas compter seulement sur WHERE tenant_id = ?.
- RLS est obligatoire.
- Les queries applicatives ne doivent pas dupliquer toute la sécurité tenant.
- Les inserts doivent renseigner tenant_id depuis le contexte serveur.
```

Exceptions :

```text
- platform scope sans tenant
- public default tenant
- super admin override audité
- jobs batch avec contexte explicite
```

## 6. Audit columns

Colonnes recommandées pour tables critiques :

```text
created_at timestamptz not null
created_by uuid null
updated_at timestamptz not null
updated_by uuid null
deleted_at timestamptz null
version bigint not null
```

Règles :

```text
created_at/updated_at = Instant
deleted_at = soft-delete si nécessaire
version = optimistic locking
created_by/updated_by = acteur applicatif si disponible
```

## 7. Timestamps

Utiliser :

```text
Instant en Java
TIMESTAMPTZ en PostgreSQL
Clock injecté pour now()
```

Ne pas utiliser :

```text
LocalDateTime pour sold_at, paid_at, synced_at, occurred_at, created_at
JVM default timezone pour règles métier
client timezone comme vérité système
```

Types autorisés :

```text
Instant      -> moment métier/persistence/API
LocalDate    -> date calendrier
LocalTime    -> horaire de schedule
ZonedDateTime -> calcul seulement, conversion vers Instant avant persistence
```

## 8. Idempotency persistence

Table obligatoire pour endpoints critiques :

```sql
create table idempotency_record (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  scope varchar(64) not null,
  idem_key varchar(160) not null,
  request_hash varchar(128) not null,
  status varchar(32) not null,
  resource_id uuid null,
  response_json jsonb null,
  expires_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by uuid null,
  constraint uq_idempotency_scope_key unique (tenant_id, scope, idem_key),
  constraint ck_idem_status check (status in ('IN_PROGRESS','COMPLETED','FAILED'))
);

create index idx_idem_expiry on idempotency_record (expires_at);
```

Semantics :

```text
same tenant/scope/key + same hash => replay same response
same tenant/scope/key + different hash => 409 payload_mismatch
missing key on required endpoint => 400 missing
IN_PROGRESS => 409 in_progress
```

## 9. Processed events

Pour consumers/projectors idempotents :

```sql
create table processed_event (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references tenant(id),
  handler_key varchar(96) not null,
  event_id uuid not null,
  processed_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  created_by uuid null,
  constraint uq_processed_event unique (tenant_id, handler_key, event_id)
);

create index idx_processed_event_lookup on processed_event (handler_key, event_id);
```

Règle :

```text
handler_key est constant dans le code.
event_id vient du DomainEvent.
Duplicate event => no-op.
```

## 10. Concurrency control

Handlers critiques ne doivent jamais se baser seulement sur un snapshot stale.

Utiliser au moins un mécanisme :

```text
optimistic locking version
select for update
unique constraint
state transition guarded update
transactional re-check
```

Exemples :

```sql
update sales_session
set status = 'CLOSED', version = version + 1
where id = :id
  and status = 'OPEN'
  and version = :expected_version;
```

Si `updated_rows = 0`, retourner conflit métier.

## 11. Contraintes uniques recommandées

```text
terminal: unique(tenant_id, code)
terminal_assignment active: unique partial(tenant_id, terminal_id) where status='ACTIVE'
device_binding active: unique partial(tenant_id, terminal_id) where status='ACTIVE'
sales_session open per terminal: unique partial(tenant_id, terminal_id) where status='OPEN'
idempotency: unique(tenant_id, scope, idem_key)
processed_event: unique(tenant_id, handler_key, event_id)
ticket public code: unique(tenant_id, public_code)
```

## 12. Entity placement

JPA entities :

```text
core.<domain>.internal.infra.persistence
```

Interdits :

```text
@Entity dans domain
repositories dans application/domain
JPA entity exposée via API ou DTO
Spring Data REST pour core critique
```

## 13. Persistence adapters

Les handlers dépendent de ports :

```text
application.port.out.TerminalReaderPort
application.port.out.TerminalWriterPort
```

Infra implémente :

```text
infra.persistence.JpaTerminalAdapter
```

Port signatures utilisent :

```text
typed IDs
domain/application records
views/projections
```

Pas :

```text
JPA entities
Repository
Page<T> exposé au web
```

## 14. Soft delete

Soft-delete si :

```text
- audit/regulatory besoin
- historique métier
- référence encore utilisée
```

Hard-delete possible si :

```text
- données temporaires expirées
- idempotency cleanup
- activation challenges expirés
- logs techniques purgeables
```

Règle :

```text
Les readers doivent filtrer deleted_at sauf flows audit/ops explicites.
```

## 15. Migrations Flyway

Chaque migration doit :

```text
- créer table
- créer contraintes
- créer indexes
- activer RLS
- créer policies
- seed minimal si nécessaire
- être idempotente uniquement si style projet l’accepte
```

Checklist migration :

- [ ] Nom clair `Vxxx__domain_change.sql`.
- [ ] `tenant_id` présent si tenant-scoped.
- [ ] Index sur colonnes lookup.
- [ ] Contrainte unique métier.
- [ ] RLS enabled.
- [ ] Policies créées.
- [ ] `created_at/updated_at/version` si mutable.
- [ ] Checks enum string.

## 16. Sécurité SQL

Interdits :

```text
string concatenation SQL avec input client
native query sans justification
manual tenant bypass
set_config hors infrastructure RLS/context
```

Obligatoire :

```text
param binding
sort allowlist
pagination DB
RLS tests
```

## 17. Test persistence minimal

Pour chaque table critique :

```text
- migration applies cleanly
- RLS blocks cross tenant read
- RLS blocks cross tenant write
- unique constraints work
- optimistic locking works
- soft-delete filters work
- audit columns set
- timestamps are Instant/timestamptz
```

## 18. PR checklist persistence

- [ ] Flyway migration présente.
- [ ] ddl-auto reste validate.
- [ ] RLS pour tables tenant-scoped.
- [ ] UUID seulement en persistence.
- [ ] Typed IDs hors persistence.
- [ ] No LocalDateTime for business moments.
- [ ] Constraints DB pour invariants critiques.
- [ ] Version/locking sur mutable critique.
- [ ] Mapper JPA explicite.
- [ ] Test RLS/concurrency/idempotency.
