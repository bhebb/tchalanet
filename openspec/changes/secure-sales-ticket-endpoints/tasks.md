## Status: DRAFT

## 1. Réactivation des @Secured (TicketController)

- [ ] 1.1 `TicketController.cancel` (ligne 207) — ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})`
- [ ] 1.2 `TicketController.print` (ligne 218) — ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})`
- [ ] 1.3 `TicketController.printEscpos` (ligne 230) — ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})`
- [ ] 1.4 `TicketController.printPdf` (ligne 239) — ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})`
- [ ] 1.5 Vérifier que les 4 imports sont déjà présents (`org.springframework.security.access.annotation.Secured`)

## 2. Standardisation `ApiResponse<T>` sur public verify

- [ ] 2.1 `PublicTicketController.verify` — retourner `ResponseEntity<ApiResponse<TicketVerificationResult>>` ; remplacer `body(res)` par `body(ApiResponse.success(res))`
- [ ] 2.2 Cas 404 (`res == null`) — utiliser `ApiResponse.notFound("Ticket not found")` au lieu de `ResponseEntity.status(404).build()`
- [ ] 2.3 Conserver les headers `X-Robots-Tag: noindex, nofollow` et `Cache-Control: no-store`
- [ ] 2.4 Mettre à jour le test `PublicTicketControllerTest` (si existant) ou créer une couverture pour vérifier la structure `ApiResponse`

## 3. Rate-limiting `/public/tickets/**`

- [ ] 3.1 Ajouter `com.bucket4j:bucket4j-core` au `pom.xml` (root + tchalanet-server)
- [ ] 3.2 Créer `common/web/ratelimit/PublicTicketsRateLimitFilter.java` — filtre Spring qui bucket par IP, mappe sous `/public/tickets/**`
- [ ] 3.3 Créer `common/config/PublicTicketsRateLimitProperties.java` — `@ConfigurationProperties(prefix = "tch.public.tickets.rate-limit")` (`enabled: boolean`, `requestsPerSecond: int`, `burst: int`)
- [ ] 3.4 Réponse 429 avec header `Retry-After` (secondes)
- [ ] 3.5 Logger les rejets avec `WARN log.warn("Rate limit exceeded ip={} path={}", ip, path)` pour audit
- [ ] 3.6 Configuration dans `application.yaml` : defaults `enabled: true, requests-per-second: 10, burst: 30`

## 4. Extension ArchUnit

- [ ] 4.1 Étendre `SecurityArchTest.java` — ajouter `/tenant/tickets/` aux path prefixes couverts ; règle : `@RestController` mappé sous ces prefixes doit avoir `@Secured` ou `@PreAuthorize` (classe ou toutes méthodes handlers publiques)
- [ ] 4.2 Documenter les path prefixes couverts dans un commentaire en tête de classe
- [ ] 4.3 Vérifier `./mvnw test -Dtest=SecurityArchTest` passe

## 5. Tests sécurité (@WebMvcTest)

- [ ] 5.1 Créer `TicketControllerSecurityTest.java` — `@WebMvcTest(TicketController.class)`
- [ ] 5.2 Scénarios 401 (sans token) sur les 4 endpoints `cancel`, `print`, `print.escpos`, `print.pdf`
- [ ] 5.3 Scénarios 403 (token sans `ROLE_CASHIER/ADMIN/SUPER_ADMIN`) sur les 4 endpoints
- [ ] 5.4 Scénarios 200/204 (token avec rôle valide) sur les 4 endpoints — couverture du happy path
- [ ] 5.5 `./mvnw test -Dtest=TicketControllerSecurityTest` passe

## 6. Tests rate-limit

- [ ] 6.1 Créer `PublicTicketsRateLimitFilterTest.java` (unitaire — pas de Spring context)
- [ ] 6.2 Créer `PublicTicketControllerRateLimitIT.java` (`@SpringBootTest` + `MockMvc`) — 11 requêtes en 1s sur `verify` → 11ème = 429 + `Retry-After` présent
- [ ] 6.3 Test : si `tch.public.tickets.rate-limit.enabled: false`, aucune limite appliquée

## 7. Spec capabilities

- [ ] 7.1 Mettre à jour `openspec/specs/auth-rbac/spec.md` avec 3 nouveaux requirements (cancel, print\*, public verify rate-limit) et leurs scénarios
- [ ] 7.2 Le delta delta `specs/auth-rbac/spec.md` du change décrit l'ajout

## 8. Documentation

- [ ] 8.1 `docs/conventions/api/web_api.md` — section ArchUnit : ajouter `/tenant/tickets/` à la liste des scopes couverts
- [ ] 8.2 `docs/conventions/api/web_api.md` — nouvelle section rate-limiting public (config keys, logging WARN, format 429)
- [ ] 8.3 Mettre à jour `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` §5 et §9 — supprimer les anomalies traitées par ce change

## 9. Vérification finale

- [ ] 9.1 Rechercher `@Secured` dans `TicketController` → 10 occurrences (toutes méthodes annotées)
- [ ] 9.2 `./mvnw clean verify` → build vert + tous tests
- [ ] 9.3 Coordonner avec front public pour adapter le parsing `ApiResponse.data` côté `verify`
- [ ] 9.4 CHANGELOG : mention `BREAKING (public API)` sur la migration `verify` vers `ApiResponse<T>`
