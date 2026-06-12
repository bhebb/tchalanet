# Tasks — Observability & Request Tracing V1

## Status

Proposed — decisions finalisées 2026-06-12

## Slices

### B1 — Backend: request ID primitives + filter

- [x] Ajouter `TchHeaders.X_TRACE_ID` et `X_SPAN_ID` dans `common.http.TchHeaders`
- [x] Créer `common.observability.RequestId` (value object, validation `^[A-Za-z0-9._:-]{8,96}$`)
- [x] Créer `common.observability.TchTraceIds` (extraction traceId/spanId depuis Micrometer context)
- [x] Créer `common.observability.TchObservabilityProperties` (config `tch.observability.*`,
      dont `exempt-paths` incluant `/public/**`, `/actuator/**`, `/swagger-ui/**`, `/api-docs/**`,
      `OPTIONS /**`, `/static/**`, `/favicon.ico`)
- [x] Créer `common.web.observability.RequiredRequestIdFilter` :
      — filtre Servlet avant Spring Security / `BearerTokenAuthenticationFilter`
      — allowlist technique/public configurable
      — valide `X-Request-Id` sur les paths non-exemptés
      — rejette avec 400 `ProblemDetail` `request_id.missing` / `request_id.invalid`
      — génère `serverRequestId` interne pour logs et `ProblemDetail` de rejet
      — ne retourne pas `serverRequestId` comme `X-Request-Id`
      — met `requestId` valide dans MDC
      — expose `requestId` comme attribut requête
      — cleanup MDC en `finally`
- [x] Créer `common.web.observability.TraceResponseHeaderFilter` :
      — retourne `X-Request-Id` depuis attribut requête ou header entrant
      — retourne `X-Trace-Id` / `X-Span-Id` si disponibles (fail-open si absent)
- [x] Fixer `GlobalErrorHandler` : renommer propriété `traceId` → `requestId` dans `decorate()`,
      déléguer l'enrichissement OTel (`traceId`/`spanId`) à `TchTraceIds`
- [x] Tests unitaires : header absent → 400, header invalide → 400, path exempté → pass, MDC cleanup
- [ ] Test intégration : header présent retourné en réponse, headers trace si OTel actif (→ après B3)

### B2 — Backend: ApiResponse trace block

- [x] Créer `common.web.api.ApiTraceInfo` record (`requestId`, `traceId`, `spanId`)
- [x] Créer `common.web.observability.ApiResponseTraceAdvice` :
      — `ResponseBodyAdvice` pour les 2xx JSON `ApiResponse`
      — retourne une vue JSON enrichie non-breaking
      — ne modifie pas le record `ApiResponse<T>`
      — ignore `ProblemDetail` / error responses
- [x] Tests : bloc `trace` présent dans 2xx, null-safe quand OTel absent,
      `ProblemDetail` non modifié par l'advice

### B3 — Backend: OTel dependencies + OTLP exporter

- [x] Ajouter `micrometer-tracing-bridge-otel` dans `tchalanet-app/pom.xml`
- [x] Ajouter `opentelemetry-exporter-otlp`
- [x] Ajouter config `application-local-ide.yaml` : OTLP endpoint `${OTEL_ENDPOINT:http://localhost:4318/v1/traces}`, sampler `1.0`
- [x] Ajouter config `application-staging.yaml` : sampler `${OTEL_SAMPLER_PROBABILITY:1.0}`, gzip, endpoint env var
- [x] Configurer l'endpoint OTLP commun et injecter l'endpoint Docker interne dans le service API
- [ ] Ajouter config `application-prod.yaml` : sampler `0.05` (fichier prod non créé — à faire lors du setup prod)
- [ ] Vérifier fail-open : collecteur down → API 200 OK sur endpoint de vente (→ test intégration après I1 up)
- [ ] Test : absence du collecteur ne change pas le status HTTP

### B4 — Backend: ObservedCommandBus / ObservedQueryBus

- [x] Créer `common.bus.observability.ObservedCommandBus implements CommandBus`, délègue vers `CommandBus` target
- [x] Créer `common.bus.observability.ObservedQueryBus implements QueryBus`, délègue vers `QueryBus` target
- [x] Ne pas dépendre directement de `SimpleCommandBus` / `SimpleQueryBus`
- [x] Configurer `tch.observability.tracing.sensitive-message-allowlist` (SellTicketCommand, etc.)
- [x] Attributs span : `tch.request_id`, `tch.command`/`tch.query`, `tch.outcome`
- [x] Tests : span créé pour commande allowlistée, pas de span pour commande non-listée, résultat de commande inchangé si traceur absent

### W1 — Web: request ID interceptor upgrade

- [x] Mettre à jour `correlation.interceptor.ts` :
      format `tch_req_${crypto.randomUUID()}` (pas de lib ULID en V1)
- [x] Lire `X-Request-Id`, `X-Trace-Id`, `X-Span-Id` depuis les headers de réponse
- [x] Attacher les infos de diagnostic à l'error model (dans `http-error.mapper.ts`)
- [x] Ajouter un type `TchDiagnosticInfo` (`requestId`, `traceId`, `spanId`) dans `@tch/api`
- [x] Ajouter `trace?: TchDiagnosticInfo` dans `ApiResponse` et `ProblemDetail`
- [x] Tests : interceptor ajoute `X-Request-Id` préfixé `tch_req_`, error model contient les trace ids

### W2 — Web: copy diagnostics UI

- [x] Identifier les composants d'erreur impactés (`ShellFeedbackBanner`, error panels, formulaires admin)
- [x] Ajouter section expandable "Détails techniques" : requestId, traceId, spanId
- [x] Implémenter bouton "Copier" avec format spec §10.2 (requestId=... traceId=... spanId=... route=... time=...)
- [x] Tests : copie contient les bons champs, section masquée par défaut

### M1 — Mobile: Dio interceptor + copy diagnostics

> Dio ^5.9.2 est déjà intégré — brancher un interceptor standard, pas introduire Dio.

- [x] Créer `core/network/request_id_interceptor.dart` (Dio `Interceptor`) :
      `onRequest` : ajoute `X-Request-Id: tch_req_<hex>` si absent
      `onError` : lit `X-Request-Id`, `X-Trace-Id`, `X-Span-Id` + enregistre dans `DiagnosticRepository`
- [x] Créer `core/observability/diagnostic_info.dart` (`requestId`, `traceId`, `spanId`, `route`, `operation`, `occurredAt`, `toCopyText()`)
- [x] Créer `core/observability/diagnostic_repository.dart` (in-memory, max 5 entries, provider Riverpod)
- [x] Enregistrer `RequestIdInterceptor` en premier dans `apiClientProvider`
- [x] Mettre à jour `ApiException` : ajouter `requestId` et `spanId`
- [x] Corriger `_extractTraceId` : utilise `X-Trace-Id` (pas `X-Request-Id`) ; ajouter `_extractRequestId` et `_extractSpanId`
- [x] Ajouter bouton "Copier diagnostic" dans l'UI erreur POS (`_ErrorBody` et inline error banner)
- [x] Format copie : `requestId=... traceId=... spanId=... operation=... time=...` (pas de PII, pas de token)
- [x] Tests : interceptor ajoute header `tch_req_`, error model capture les trace ids, copie sans PII, éviction après 5 entrées

### I1 — Infra: local OTel Collector + Jaeger

- [x] Créer `compose/docker-compose-otel.yml` (OTel Collector + Jaeger all-in-one)
- [x] Créer `compose/otel/otel-collector-config.yaml` (OTLP receiver → batch → Jaeger, PII attribute filter)
- [x] Ajouter un exporter Collector `debug` local pour diagnostiquer les spans reçus dans les logs
- [x] Ajouter stubs de config staging (Grafana Cloud OTLP placeholder commenté dans collector config)
- [x] Mettre à jour `compose/docker-compose.index.md` (table + quick-start OTel section)
- [x] Valider : `docker compose config` passe avec docker-compose-project.yml
- [x] Valider end-to-end : server exporte vers collector, traces visibles dans Jaeger UI (→ runtime test)

## Ordre d'exécution recommandé

```
1. B1  — Request ID backend (primitives + filter + MDC + GlobalErrorHandler fix)
2. W1  — Web interceptor (format + response headers + error model)
3. M1  — Mobile interceptor (Dio, X-Request-Id, error model)
4. B2  — Trace block ApiResponse / ProblemDetail enrichment
5. B3  — OTel deps + OTLP exporter config
   I1  — Local OTel Collector + Jaeger docker compose
6. B4  — ObservedCommandBus / ObservedQueryBus spans
7. W2  — Web copy diagnostics UI
   M1  — Mobile copy diagnostics UI (complément slice M1)
```

Rationale : B1 apporte de la valeur immédiate (request id traçable dans les logs) sans attendre
la stack OTel. W1/M1 permettent de valider le contrat HTTP de bout en bout avant d'ajouter les
traces OTel. B3+I1 débloquent Jaeger local. B4 et les UI copy viennent en dernier.

## Acceptance criteria (spec §18)

- [ ] Web envoie `X-Request-Id` sur les appels backend non-exemptés
- [ ] Mobile envoie `X-Request-Id` sur les appels backend non-exemptés
- [ ] Backend rejette `X-Request-Id` absent/invalide sur endpoints protégés
- [ ] Backend retourne `X-Request-Id` dans chaque réponse avec request id valide
- [ ] Backend retourne `X-Trace-Id` et `X-Span-Id` quand disponibles
- [ ] `ProblemDetail` contient `requestId`/`traceId`/`spanId` quand disponibles
- [ ] `ProblemDetail` `request_id.missing` contient `serverRequestId`
- [ ] `ApiResponse` 2xx contient bloc `trace` ou équivalent documenté
- [ ] Frontend peut copier les détails de diagnostic
- [ ] Mobile peut copier les détails de diagnostic
- [x] Jaeger local affiche les traces backend
- [ ] Flows sensibles cashier/admin créent des spans significatifs
- [ ] Panne collecteur ne casse pas l'API
- [ ] Pas de PII, token, payload sensible ou secrets dans spans/logs
