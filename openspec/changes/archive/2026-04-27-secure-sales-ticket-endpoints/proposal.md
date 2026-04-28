## Why

L'audit `2026-04-26-sales-pipeline-audit.md` (§Sécurité) a identifié 4 endpoints du `TicketController` sans annotation `@Secured` : `PATCH /tenant/tickets/{id}/cancel`, `GET /tenant/tickets/{id}/print`, `GET /tenant/tickets/{id}/print.escpos`, `GET /tenant/tickets/{id}/print.pdf`. Tout principal authentifié — même sans rôle métier — peut annuler un ticket ou en réimprimer un duplicata. Par ailleurs, `PublicTicketController.verify` retourne `ResponseEntity<?>` raw au lieu d'`ApiResponse<T>`, et aucun rate-limiting n'est implémenté sur les endpoints publics malgré le commentaire d'intention dans `VerifyPublicTicketQueryHandler`. Sans ces colmatages, un cashier sans habilitation peut détourner des tickets, et la surface publique est exposée au brute-force sur `publicCode`.

## What Changes

- **[Auth `cancel`]** Ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})` sur `TicketController.cancel` (ligne 207) — même périmètre que `sell`, le cashier peut annuler dans la fenêtre limites/autonomie.
- **[Auth `print*`]** Ajouter `@Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})` sur les 3 endpoints `print`, `print.escpos`, `print.pdf` — un duplicata physique est sensible (preuve de jeu).
- **[`ApiResponse` public verify]** `PublicTicketController.verify` retourne `ApiResponse<TicketVerificationResult>` au lieu de `ResponseEntity<?>` raw ; conserver les headers `X-Robots-Tag: noindex, nofollow` + `Cache-Control: no-store` ; 404 → `ApiResponse.notFound(...)`.
- **[Rate limiting]** Activer un filtre rate-limit (Bucket4j ou équivalent déjà utilisé dans `common.web`) sur `/public/tickets/verify/**` et `/public/tickets/qr/**` — limite par IP : 10 req/s, burst 30 ; configurable via `tch.public.tickets.rate-limit.*`.
- **[ArchUnit `/tenant/tickets/**`]** Étendre `SecurityArchTest`(créée par`secure-draw-ops-endpoints`) : tout `@RestController`mappé sous`/tenant/tickets/`doit avoir`@Secured`(ou`@PreAuthorize`) au niveau classe ou sur chaque méthode handler ; échec build sinon.
- **[Tests sécurité]** `TicketControllerSecurityTest.java` (`@WebMvcTest`) — 4 nouveaux scénarios 401/403 sur `cancel`, `print`, `print.escpos`, `print.pdf`.
- **[Tests rate-limit]** `PublicTicketControllerRateLimitTest.java` — 11 requêtes en 1 s sur `verify` → la 11ème reçoit 429 Too Many Requests.

## Capabilities

### Modified Capabilities

- `auth-rbac`: Étension du périmètre couvert par la règle ArchUnit pour inclure `/tenant/tickets/**` ; ajout des requirements explicites pour les endpoints `cancel` et `print*` du `TicketController` ; ajout d'un requirement pour le rate-limiting des endpoints publics `/public/tickets/**`.

## Impact

- **Code modifié** : `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/infra/web/TicketController.java`, `PublicTicketController.java`
- **Code créé** : `TicketControllerSecurityTest.java`, `PublicTicketControllerRateLimitTest.java`, configuration filtre rate-limit dans `common.web`
- **Code modifié (test)** : `SecurityArchTest.java` — extension du scope (path `/tenant/tickets/`)
- **Spec mise à jour** : `openspec/specs/auth-rbac/spec.md` (3 nouveaux requirements)
- **Docs** : `tchalanet-server/docs/conventions/api/web_api.md` (mention du nouveau scope ArchUnit)
- **Configuration** : `application.yaml` — `tch.public.tickets.rate-limit.*` (defaults documentés)
- **API** : aucun changement de contrat tenant ; le format `verify` change pour `ApiResponse<T>` (clients publics doivent extraire `.data`) — communiqué dans CHANGELOG
- **Non scope** : sécurité d'autres controllers du domaine sales hors `/tenant/tickets/**` ; refonte du modèle de rôles ; passage de `@Secured` à `@PreAuthorize` (autre style autorisé par le projet, traité dans un change de standardisation séparé)
