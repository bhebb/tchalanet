# OpenSpec Change — Observability & Request Tracing V1

## Status

Proposed — 2026-06-12

## Why

Tchalanet n'a pas de traçabilité systématique des requêtes.

Le `GlobalErrorHandler` lit déjà `X-Request-Id` mais :
- le stocke sous la propriété `traceId` dans `ProblemDetail` (bug de nommage) ;
- ne rejette pas les requêtes sans header sur les endpoints protégés ;
- ne valide pas le format ;
- ne propage pas le `requestId` dans le MDC.

Le web possède un `correlationRequestInterceptor` mais :
- génère un UUID sans préfixe (non conforme au format recommandé) ;
- ne lit pas les headers de réponse `X-Trace-Id` / `X-Span-Id` ;
- n'expose pas les infos de diagnostic à l'UI.

Il n'existe pas de stack OTel locale, pas de `ObservedCommandBus` / `ObservedQueryBus`, et aucun bloc `trace` dans `ApiResponse`.

## What

Mettre en place une observabilité simple, fail-open et non bloquante :

1. **`X-Request-Id` obligatoire** sur les endpoints protégés avec rejet 400 si absent/invalide.
2. **MDC propagation** : `requestId`, `traceId`, `spanId`, `tenantId`, `scope`, `userId`, etc.
3. **Headers de réponse** : `X-Request-Id`, `X-Trace-Id`, `X-Span-Id`.
4. **`ProblemDetail` enrichi** : `requestId`, `traceId`, `spanId` (fix bug nommage existant).
5. **`ApiResponse` enrichi** : bloc `trace` optionnel via `ResponseBodyAdvice`.
6. **OTel OTLP exporter** : Micrometer Tracing bridge, export async vers collecteur local.
7. **`ObservedCommandBus` / `ObservedQueryBus`** : spans pour les commandes/queries sensibles.
8. **Web interceptor upgradé** : format `tch_req_<prefix>`, lecture des headers de réponse, copie diagnostics.
9. **Mobile Dio interceptor** : `X-Request-Id`, lecture des headers, copie diagnostics.
10. **Infra locale** : OTel Collector + Jaeger all-in-one docker compose.

## Impact

### Backend (`tchalanet-server`)

- Nouveau package `common.observability` :
  - `RequestId` (value object, format validation)
  - `TchTraceContext` (accès MDC + OTel context)
  - `TchTraceIds` (extraction traceId/spanId depuis Micrometer)
  - `TchTraceHeaders` (constantes)
  - `TchObservationTags` (attributs span autorisés)
- Nouveau package `common.web.observability` :
  - `RequiredRequestIdFilter` (rejet 400, MDC, cleanup)
  - `TraceResponseHeaderFilter` (headers réponse)
  - `ProblemDetailTraceCustomizer` (enrichit ProblemDetail — remplace logique partielle dans GlobalErrorHandler)
  - `ApiResponseTraceAdvice` (ResponseBodyAdvice — bloc `trace` dans ApiResponse)
- Nouveau package `common.bus.observability` :
  - `ObservedCommandBus` (wrapper, allowlist config-driven)
  - `ObservedQueryBus` (wrapper, allowlist config-driven)
- `TchHeaders` : ajout `X_TRACE_ID`, `X_SPAN_ID`
- `GlobalErrorHandler` : fix nommage `traceId` → `requestId`, déléguer enrichissement OTel
- `pom.xml` : `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`
- `application-local.yaml` / stg / prod : config OTLP

### Web (`tchalanet-web`)

- `correlation.interceptor.ts` : format `tch_req_<prefix>`, lecture `X-Trace-Id` / `X-Span-Id`
- `problem-detail.interceptor.ts` ou `api-feedback.interceptor.ts` : attacher diagnostics à l'error model
- Composant erreur / `ShellFeedbackBanner` : section "Détails techniques" avec copie

### Mobile (`tchalanet-mobile`)

- Nouveau Dio interceptor : `X-Request-Id` (`mob_req_<prefix>`), lecture headers
- Error model : capturer `requestId`, `traceId`, `spanId`
- UI erreur POS : bouton "Copier diagnostic"

### Infra (`tchalanet-infra`)

- `docker-compose-otel.yml` : OTel Collector + Jaeger all-in-one
- `otel-collector-config.yaml` : pipeline local 100% sampling
- Update `docker-compose.index.md`
- Config OTLP stg/prod (placeholders Grafana Cloud)

## Non-goals

- SIEM ou stratégie complète d'audit sécurité.
- Remplacement de `platform.audit` par les traces.
- Tracing manuel de chaque méthode métier.
- Stockage de payloads complets dans les traces.
- Vendor lock-in vers Jaeger/Grafana/Honeycomb.
- Modifier le contrat de `ApiResponse` en changeant la signature du record Java.

## Decisions

```
[DECIDED] ApiResponse trace metadata uses ApiResponseTraceAdvice (ResponseBodyAdvice) in V1.
          Rationale: zero impact sur les factory methods et les callers existants. Le record
          ApiResponse<T> n'est pas modifié. V2 pourra ajouter le champ quand les factories
          seront stabilisées.

[DECIDED] Web request id = tch_req_${crypto.randomUUID()} in V1.
          Rationale: crypto.randomUUID() est disponible sans lib supplémentaire. Le pattern
          backend ^[A-Za-z0-9._:-]{8,96}$ accepte ce format. ULID/UUIDv7 évalué en V2 si
          tri lexical temporel devient nécessaire.

[DECIDED] Mobile request id = tch_req_${uuid} (Dart uuid package ou équivalent).
          Idempotency-Key reste séparé et utilisé uniquement sur les opérations critiques
          retryables (sell, payout, offline sync).

[DECIDED] RequiredRequestIdFilter s'exécute avant Spring Security (filtre Servlet, pas
          filtre Security). Allowlist pour exempter :
            OPTIONS /**
            /actuator/**
            /swagger-ui/**
            /api-docs/**
            /public/**
            /static/**
            /favicon.ico
          Pour les endpoints protégés : X-Request-Id absent/invalide → 400 ProblemDetail.
          Le backend peut générer un server_request_id interne pour les logs de rejet.

[DECIDED] Mobile : Dio est déjà intégré (dio ^5.9.2 dans pubspec.yaml). La tâche M1
          consiste à ajouter/brancher un interceptor Dio standard, pas à introduire Dio.

[DECIDED] X-Request-Id change de statut :
          Ancienne règle : optional, le serveur peut en générer un si absent.
          Nouvelle règle V1 : REQUIRED sur les endpoints protégés.
          Pour les endpoints exemptés (public/technique), le backend peut générer un
          server_request_id interne mais ne rejette pas.
```

## Relation avec les specs existantes

`external-auth-managed-postgres-observability-v0` Phase 5 est partiellement supersedée :

- **Supersedé par ce change (B3 + I1)** : OTel Collector docker, Jaeger, OTLP exporter,
  `X-Request-Id` contract, `traceId`/`spanId` propagation, frontend/mobile diagnostics,
  local/stg/prod visualization, CommandBus/QueryBus tracing générique.
- **Reste valide dans Phase 5** : attributs de trace spécifiques Firebase/identity,
  identity bootstrap spans, AppUser/external identity correlation, auth failure diagnostics,
  identity latency metrics.

## Constraints

- Observabilité fail-open : si le collecteur est down, l'API reste UP.
- Export traces asynchrone uniquement — jamais dans le chemin de requête.
- Pas de PII ni de secrets dans les spans/logs (voir spec §15.2).
- `RequiredRequestIdFilter` s'exécute avant `BearerTokenAuthenticationFilter` (filtre Servlet).
- `ApiResponse<T>` record non modifié en V1 — injection via `ResponseBodyAdvice` uniquement.
