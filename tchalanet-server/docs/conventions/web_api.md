# Backend Web API Conventions & Style (Server)

> Document canonique des conventions et du style des contrôleurs REST backend.
> Couvre: scopes/routing, permissions, signatures, DTO Request/Response, pagination (voir `../PAGINATION.md`), enveloppe de réponse, dispatch Command/Query.

## Scopes & Routing (MUST)

- Public: `/public/**`
- Tenant: `/tenant/**`
- Admin: `/admin/**`
- Platform: `/platform/**`
- SDR: `/_sdr/**` (isolation)

Voir référence longue: `../ROUTING_AND_API_PATHS_V1.md`.

## DTO rules (MUST)

- Web DTOs MUST NOT leak infra entities.
- MapStruct for stable mapping DTO <-> domain views.
- IDs in web DTOs use wrappers or strings; never expose raw UUID fields unless already a wrapper string.
- Naming convention: request DTOs end with `XxxRequest`; response DTOs end with `XxxResponse`.

## Controllers — Responsibilities (MUST)

- Validation + mapping + call handler (CommandBus/QueryBus) + build response envelope.
- No business logic; no manual authorization logic in controllers.

## Permissions & Security (MUST)

- Use `@Secured({"ROLE_..."})` or `@PreAuthorize(...)`.
- Fine-grain permission evaluation via `TchPermissionEvaluator` + `CheckUserPermissionsHandler`.
- Controllers express requirements; domain decides.

## IDs (MUST)

- Controllers MUST use typed wrappers in path/query params (e.g., `@PathVariable TicketId`, `@RequestParam UserId`).
- Converters live in `common.web.converter` (String -> Wrapper).
- UUID is allowed ONLY in persistence layer (JPA entities, repositories, Flyway).

## Pagination (MUST)

- List endpoints MUST return `ApiResponse<TchPage<XxxResponse>>`.
- Controller takes: `@TchPaging(...) TchPageRequest pageReq`.
- Query uses: `page = pageReq.pageable().getPageNumber()`, `size = pageReq.pageable().getPageSize()`.
- QueryBus returns `TchPage<T>` (not Spring `Page`).

Règle canonique: `../PAGINATION.md`.

## Response envelope (MUST)

All successful JSON endpoints MUST return `ApiResponse<T>`.

- Success: `return ApiResponse.success(data);` (HTTP 200)
- Created: `return ApiResponse.created(data);` (HTTP 201)
- Pending/Async: `return ApiResponse.pending(notice, data);` (HTTP 202)
- Warnings: `return ApiResponse.warn(data, notices);` (HTTP 200)
- Partial: `return ApiResponse.partial(data, services, notices);`

DO NOT return raw DTOs without ApiResponse.

Voir détails: `../API_RESPONSE_STANDARDIZATION.md`.

## Command / Query dispatch (MUST)

- All use cases MUST be executed via `CommandBus.send(cmd)` / `QueryBus.send(q)`.
- No direct service invocation if a handler exists.

## Style — Patterns standard

- Success: `ApiResponse.success(payload)`.
- Created: `ApiResponse.created(payload)` (+ 201).
- Pending: `ApiResponse.pending(notice, payload)` (+ 202).
- Lists: `ApiResponse.success(TchPage<XxxResponse>)`.

## Print endpoints

- `byte[]` endpoints allowed for PDF/ESC-POS.
- Always set headers: `Cache-Control: no-store` and `Content-Disposition: inline; filename=...`.

---

Ce fichier présente la convention et le style backend. Pour les détails d’implémentation (exemples complets, flows), voir les références:

- Routing & scopes: `../ROUTING_AND_API_PATHS_V1.md`
- API response: `../API_RESPONSE_STANDARDIZATION.md`
- Pagination (canonique): `../PAGINATION.md`
