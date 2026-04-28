## Context

`TicketController` (`/tenant/tickets`) utilise `@Secured` granulaire par méthode (cohérent avec le pattern Spring Security du projet). Sur 10 endpoints, 4 ont été oubliés :

- `PATCH /{id}/cancel` (ligne 207)
- `GET /{id}/print` (ligne 218) — retourne PDF en Base64
- `GET /{id}/print.escpos` (ligne 230) — bytes ESC/POS
- `GET /{id}/print.pdf` (ligne 239) — PDF binaire

Ces endpoints sont accessibles à tout principal authentifié (Spring Security autorise par défaut tout utilisateur authentifié si aucune annotation n'est posée).

`PublicTicketController.verify` retourne `ResponseEntity<?>` raw avec `body(res)` ou `status(404).build()` — surface API non standard, divergence avec le reste du codebase qui utilise `ApiResponse<T>`.

`VerifyPublicTicketQueryHandler` JavaDoc ligne 28 mentionne `Rate-limit at controller layer to prevent abuse` — non implémenté.

## Goals / Non-Goals

**Goals:**

- Restaurer l'annotation `@Secured` manquante sur les 4 endpoints du `TicketController`
- Standardiser `PublicTicketController.verify` sur `ApiResponse<TicketVerificationResult>`
- Activer un rate-limit IP-based sur `/public/tickets/**` (verify + QR)
- Étendre `SecurityArchTest` au scope `/tenant/tickets/**` pour empêcher la régression
- Couvrir les 4 endpoints par des tests `@WebMvcTest` 401/403

**Non-Goals:**

- Refonte du modèle de rôles (RBAC) — `ROLE_CASHIER/ADMIN/SUPER_ADMIN` réutilisés tels quels
- Migration vers `@PreAuthorize` (cohérence avec les autres méthodes du même controller : on garde `@Secured`)
- Audit sécurité d'autres controllers du domaine sales (`SalesTicketAdminAdapter` est un bridge, pas un controller HTTP)
- Sécurisation d'autres controllers `/tenant/**` hors `tickets`

## Decisions

### D1 — Périmètre des rôles sur `cancel`

Trois options :

1. `ROLE_CASHIER, ROLE_ADMIN, ROLE_SUPER_ADMIN` (même que sell)
2. `ROLE_ADMIN, ROLE_SUPER_ADMIN` (cancel comme opération sensible)
3. `ROLE_CASHIER` avec délégation autonomy/limits côté handler

**Décision** : option 1 — alignement avec sell. Le `CancelSaleCommandHandler` ré-évalue déjà les limites avec `OperationType.CANCEL` et délègue à `ResolveAutonomyPolicyService` pour le BLOCK → autonomie/approval policy gère le contrôle métier ; le contrôle de rôle est juste la barrière d'accès au workflow.

### D2 — Rôles sur `print*`

**Décision** : `ROLE_CASHIER, ROLE_ADMIN, ROLE_SUPER_ADMIN` — un cashier doit pouvoir réimprimer le ticket qu'il vient d'émettre. Pas de filtre par "ticket appartient à ma session" pour l'instant (fenêtre opérationnelle MVP). Une restriction owner/session pourra être ajoutée dans un futur change.

### D3 — `ApiResponse<T>` sur public verify

Précédent : `secure-draw-ops-endpoints` n'a pas standardisé `ResponseEntity` raw → décision laissée à chaque change.

**Décision** : passer à `ApiResponse<TicketVerificationResult>` même en public. Aligne sur la convention `ApiResponse<T>` non-négociable backend. Le client public devra extraire `.data` ; impact migrationnel mineur (un seul consommateur connu : front public verify).

### D4 — Implémentation du rate-limit

Trois options :

1. Filtre Spring + `Bucket4j` (lib éprouvée, in-memory ou Redis)
2. Annotation custom `@RateLimited(...)` sur la méthode
3. Délégation à Traefik (infra)

**Décision** : option 1 — `Bucket4j` in-memory pour MVP, configurable. Backend portable (testable en intégration) ; possibilité de passer Redis-backed plus tard si scaling horizontal multi-instance. Traefik est une couche en plus mais la défense en profondeur côté app reste utile.

Configuration :

- `tch.public.tickets.rate-limit.enabled: true`
- `tch.public.tickets.rate-limit.requests-per-second: 10`
- `tch.public.tickets.rate-limit.burst: 30`
- Réponse : 429 Too Many Requests + `Retry-After` header

### D5 — Scope ArchUnit étendu

`SecurityArchTest` (créée par `secure-draw-ops-endpoints`) couvre `/admin/`, `/platform/`, `/_sdr/`. On ajoute `/tenant/tickets/`.

**Décision** : étendre la règle existante (ne pas dupliquer un test). Ajouter un commentaire dans le test listant explicitement les path prefixes couverts pour faciliter les futurs ajouts.

Alternative considérée : couvrir tout `/tenant/**` — rejeté pour ce change (scope trop large, beaucoup de controllers tenant nécessitent un audit individuel avant d'être placés sous arch test).

## Risks / Trade-offs

- **[Risque] Régression cashier** : un cashier sans `ROLE_CASHIER` (mais authentifié) qui appelait `cancel` avant aujourd'hui recevra 403. → Mitigation : audit Keycloak realm pour confirmer que tous les cashiers actifs portent bien `ROLE_CASHIER` ; CHANGELOG explicite.
- **[Risque] Rate-limit faux positifs** : 10 req/s peut être trop bas pour un POS qui scanne plusieurs tickets en burst. → Mitigation : config externalisée, valeurs ajustables sans rebuild ; le burst à 30 absorbe les pics courts.
- **[Risque] Cassure client public verify** : passage de `ResponseEntity<?>` raw à `ApiResponse<T>` change le format JSON. → Mitigation : un seul client connu (front public) ; coordination + CHANGELOG ; test e2e avant déploiement.
- **[Trade-off] Bucket4j in-memory** : non distribué entre instances. → Acceptable v1 ; suivi via Redis-backed dans un change ultérieur.

## Migration Plan

1. Ajouter dépendance `com.bucket4j:bucket4j-core` dans `pom.xml` (scope `runtime`)
2. Ajouter `@Secured` sur les 4 endpoints (1 commit séparé pour audit clair)
3. Créer le filtre rate-limit + configuration
4. Migrer `PublicTicketController.verify` vers `ApiResponse<T>`
5. Étendre `SecurityArchTest`
6. Créer les tests sécurité + rate-limit
7. `./mvnw test -pl . -am` → build + tests doivent passer
8. Coordonner front public pour adapter le parsing `ApiResponse.data`

Rollback : retirer les `@Secured` (commit isolé) si bug critique en prod ; le filtre rate-limit est désactivable via `tch.public.tickets.rate-limit.enabled: false`.

## Open Questions

- Q1 : Le rôle exact en prod est-il `ROLE_CASHIER` ou `CASHIER` ? (impact `@Secured` value : Spring Security `@Secured` exige le préfixe `ROLE_` par défaut ; à vérifier dans `SecurityConfig`).
- Q2 : Faut-il logger les 429 du rate-limit avec l'IP source pour détecter brute-force sur `publicCode` ? (probable yes, aligner avec `core.audit`).
- Q3 : Le QR PNG (`/public/tickets/qr/{publicCode}.png`) doit-il aussi avoir un rate-limit séparé (déjà cacheable HTTP 1h, donc pression réelle moindre) ou le même bucket ? Réponse provisoire : même bucket pour simplicité.
