# Idempotency (HTTP Contract)

## Header

- `Idempotency-Key` (string, recommandé UUID)

## Rule

- Même `(tenant, scope, key)` + même payload de requête DOIT être traité une seule fois et DOIT renvoyer le même résultat (replay).
- Même clé avec payload différent DOIT échouer avec `409 idempotency.payload_mismatch`.

## Required endpoints (MVP)

- `POST /api/v1/tenant/tickets` (SellTicket) -> REQUIS

## Errors (ProblemDetail codes)

- `idempotency.missing` (400)
- `idempotency.payload_mismatch` (409)
- `idempotency.in_progress` (409)

## Hashing

- Le serveur calcule `request_hash = sha256(normalized_json(body))`
- Normalisation : trier récursivement les clés des objets ; conserver l'ordre des tableaux.

## Storage & retention (informative)

- Le serveur persiste les enregistrements d'idempotence avec un TTL (`expires_at`) et purge régulièrement les lignes expirées.
