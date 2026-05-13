# Idempotence

Ce document décrit les deux couches d’idempotence utilisées dans Tchalanet :

- HTTP Idempotency (contrat API) — protège les endpoints write contre le double-submit client / réseau.
- Event Idempotence (projectors / handlers) — protège les projections (facts / read models) contre la redelivery d’événements (at-least-once).

Ces deux mécanismes se ressemblent mais ne se remplacent pas.

---

## 1) HTTP Idempotency (contrat API)

### Header

- `Idempotency-Key : string` (recommandé : UUID)

### Règle

- Même tuple `(tenant, scope, idem_key)` et même payload (hash) DOIT être traité une seule fois et DOIT renvoyer le même résultat (replay).
- Même clé avec payload différent DOIT échouer avec `409 idempotency.payload_mismatch`.
- Si une requête avec la même clé est déjà en cours, DOIT échouer avec `409 idempotency.in_progress`.

### Endpoints requis (MVP)

- `POST /api/v1/tenant/tickets` (SellTicket) : REQUIS

### Codes d'erreur (ProblemDetail)

- `idempotency.missing` (400)
- `idempotency.payload_mismatch` (409)
- `idempotency.in_progress` (409)

### Hashing

Le serveur calcule :

```
request_hash = sha256(normalized_json(body))
```

Normalisation JSON :

- trier récursivement les clés des objets
- conserver l’ordre des tableaux
- sérialiser en JSON canonique puis sha256 (hex)

> Exemple : utilisez une fonction utilitaire server-side qui prend un objet (Map/record) et renvoie le SHA256 hex de sa sérialisation canonique.

### Stockage (table)

Table : `idempotency_record` (tenant-scoped via RLS).

Contrainte d’unicité :

```
UNIQUE (tenant_id, scope, idem_key)
```

Champs minimaux :

- `tenant_id` (invisible pour les queries côté app, fournis par le contexte / RLS)
- `scope` (string)
- `idem_key` (string)
- `request_hash` (string)
- `status` : `IN_PROGRESS` | `COMPLETED` | `FAILED`
- `response_json` (jsonb) — réponse pour replay
- `resource_id` (optionnel)
- `expires_at` (TTL)

#### Retention & purge

- `expires_at` est obligatoire.
- Un job de purge peut supprimer les lignes expirées :

```sql
DELETE FROM idempotency_record WHERE expires_at < now();
```

### Implémentation (AOP)

Pattern recommandé : un aspect / filtre AOP `@RequireIdempotency(scope = ...)` ou un `HandlerInterceptor`.

Pseudo‑flow :

1. Lire le header `Idempotency-Key` (s’il manque -> `idempotency.missing`).
2. Calculer `request_hash` (SHA256 de JSON canonique).
3. `store.begin(scope, key, request_hash, ttl)` :
   - si `payload_mismatch` -> 409
   - si `in_progress` -> 409
   - si `already_completed` -> replay `response_json`
   - sinon -> proceed()
4. Après la méthode : `store.completeSuccess(responseJson)` (persist response pour replay)
5. En cas d’exception : `store.completeFailed(...)`

> Important : `completeSuccess` / `completeFailed` sont obligatoires pour garantir le replay.

### RLS (non négociable)

- Interdit de filtrer `tenant_id` dans les queries applicatives.
- `tenant_id` est écrit via `BaseTenantEntity` / `TchContext` et RLS garantit l’isolement.

---

## 2) Event Idempotence (projectors / AFTER_COMMIT)

### Objectif

Garantir qu’un événement déjà appliqué par un projector n’est jamais appliqué deux fois.

C’est indispensable pour les read models incrémentaux (ex : `draw_exposure`) car ces projectors effectuent des opérations de type :

```
stake_total = stake_total + delta
sales_count = sales_count + delta
```

### Règle

- Même tuple `(tenant, handler_key, event_id)` doit être appliqué **au plus une fois**.
- En cas de redelivery / replay : `noop` (aucune erreur côté client).

### Stockage (table)

Table : `processed_event` (tenant-scoped via RLS)

Contrainte d’unicité :

```
UNIQUE (tenant_id, handler_key, event_id)
```

Champs minimaux :

- `handler_key` (string stable, constant dans le code du projector)
- `event_id` (UUID de l’événement)
- `processed_at` (timestamptz)
- audit léger (`created_by`, etc.)

### Convention `handler_key`

- Chaîne stable au format : `<domain>.<projection>` ou `<domain>.<feature>.<projection>`
- Exemples :
  - `limitpolicy.exposure`
  - `sales.ticket_stats`
  - `payouts.daily_stats`

### Implémentation (ports / adapters)

- Port : `ProcessedEventPort` (package `common.idempotency.event`)
- Adapter JDBC : `ProcessedEventJdbcAdapter`

API :

```java
boolean alreadyProcessed(String handlerKey, UUID eventId);
void markProcessed(String handlerKey, UUID eventId);
boolean markProcessedIfAbsent(String handlerKey, UUID eventId);
```

Les nouveaux consommateurs cross-domain doivent utiliser `markProcessedIfAbsent(...)`
pour eviter le pattern non atomique `alreadyProcessed(...)` puis `markProcessed(...)`.

#### RLS (non négociable)

- Interdit : `WHERE tenant_id = ?` dans `alreadyProcessed` (la requête NE doit PAS contenir tenant filter)
- Autorisé : insérer `tenant_id` depuis `TchContext` dans `markProcessed`
- Pour les jobs async/batch : binder un `TchRequestContext` avant d’accéder à la DB

---

## 3) Quand utiliser quoi ?

### HTTP Idempotency

- Pour les endpoints write exposés aux clients
- Protège contre : double submit, retries, offline sync
- Permet de replay la même réponse

### Event Idempotence

- Pour les projectors, listeners, handlers `AFTER_COMMIT`
- Protège les read models dans un flux at-least-once
- Comportement : skip si déjà traité (no-op) — aucune erreur client

---

## Annexes / Exemples

### DDL (processed_event)

```sql
CREATE TABLE processed_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  handler_key varchar(96) NOT NULL,
  event_id uuid NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  CONSTRAINT uq_processed_event UNIQUE (tenant_id, handler_key, event_id)
);

CREATE INDEX idx_processed_event_lookup ON processed_event (handler_key, event_id);
```

---

> RLS note : on **ne** filtre pas `tenant` dans les queries applicatives. Le champ `tenant_id` existe pour la contrainte unique et pour l'audit — RLS empêche le cross-tenant access.

---
