
# Platform Capability `platform.idempotence`

`platform.idempotence` gère la sécurité anti-rejeu pour les écritures HTTP et les consommateurs d’événements.

**Ce module fait** :
- HTTP Idempotency (contrat API) : protège les endpoints write contre le double-submit client/réseau
- Event Idempotency (handlers/projectors) : protège les projections contre la redelivery d’événements

**Ce module ne fait pas** :
- Ne remplace pas les transitions d’état métier, verrous, contraintes uniques, invariants métier


## Surface API et intégration

- Header HTTP `Idempotency-Key` (UUID recommandé)
- Contrôle du tuple `(tenant, scope, idem_key, payload_hash)`
- Codes d’erreur explicites (`idempotency.missing`, `idempotency.payload_mismatch`, `idempotency.in_progress`)
- Stockage en table `idempotency_record` (tenant-scoped, RLS)

## Règles et limitations

- Ne remplace pas les contrôles métier ou les verrous
- Les handlers d’event doivent être idempotents
- La logique de hash et de normalisation JSON doit être canonique côté serveur

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

- Port : `ProcessedEventPort` (package `platform.idempotence.api`)
- Adapter JDBC : `ProcessedEventJdbcAdapter`

API :

```java
boolean alreadyProcessed(String handlerKey, UUID eventId);
void markProcessed(String handlerKey, UUID eventId);
boolean markProcessedIfAbsent(String handlerKey, UUID eventId);
```

Les nouveaux consommateurs cross-domain doivent utiliser `markProcessedIfAbsent(...)`
pour eviter le pattern non atomique `alreadyProcessed(...)` puis `markProcessed(...)`.

`handler_key` est une constante de code. Il ne vient jamais du client ou d'un payload externe.

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

## 4) Guardrails

- `@RequireIdempotency` est obligatoire sur les writes dangereux, notamment sell ticket.
- Même clé + même payload rejoue le résultat sans réexécuter la commande.
- Même clé + payload différent renvoie `idempotency.payload_mismatch`.
- Même clé + requête en cours renvoie `idempotency.in_progress`.
- Les consumers event-driven utilisent des handler keys stables et `markProcessedIfAbsent`.
- Les lignes expirées ont un comportement documenté; les invariants métier restent obligatoires.
- Les tables idempotency/processed-event appartiennent à `platform.idempotence`, pas à `common`.
