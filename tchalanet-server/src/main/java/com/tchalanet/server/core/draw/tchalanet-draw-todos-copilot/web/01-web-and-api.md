# TODO — `core.draw` web / API

## Files principaux

- `DrawAdminController`
- `features.ops.infra.web.draws.*`
- request/response DTOs draw
- web mappers
- Swagger annotations
- security annotations
- audit annotations

## Règles communes

- [ ] Controllers thin : pas de logique métier.
- [ ] Controllers appellent `CommandBus` / `QueryBus`.
- [ ] Responses via `ApiResponse<T>`.
- [ ] Errors via `ProblemRest` / `ProblemDetail`.
- [ ] `@PreAuthorize` sur endpoints admin/ops.
- [ ] `@AuditLog` pour actions sensibles.
- [ ] Ne pas exposer raw domain object si DTO web requis.
- [ ] Ne pas exposer IDs internes dans endpoints publics.

## P0 — endpoints admin draw à garder

Garder les endpoints orientés read/operations :

- [ ] `GET /admin/draws`
- [ ] `GET /admin/draws/{id}`
- [ ] `GET /admin/draws/today`
- [ ] `GET /admin/draws/upcoming` ou `/next`
- [ ] `GET /admin/draws/latest-with-results`
- [ ] `POST /admin/draws/{id}/cancel`
- [ ] `POST /admin/draws/{id}/reschedule`
- [ ] `POST /admin/draws/{id}/lock`
- [ ] `POST /admin/draws/{id}/unlock`
- [ ] `POST /admin/draws/{id}/archive`
- [ ] `POST /admin/draws/{id}/correct-result`
- [ ] `POST /admin/draws/{id}/settle` si encore exposé, mais marqué ops/admin strict.

## P0 — endpoints à supprimer/déprécier

- [ ] Supprimer/deprecate `POST /admin/draws` create manual si non slot-driven.
- [ ] Supprimer/deprecate `PUT /admin/draws/{id}` update vague.
- [ ] Supprimer/deprecate `POST /admin/draws/{id}/override` vague/no-op.
- [ ] Supprimer `/admin/draws/{id}/result` si `GetDrawById` retourne déjà result summary.

## P0 — ops endpoints

Ops doit orchestrer les jobs manuels :

- [ ] `POST /ops/draws/generate`
- [ ] `POST /ops/draws/open-due`
- [ ] `POST /ops/draws/close-due`
- [ ] `POST /ops/draw-results/apply-window`
- [ ] `POST /ops/draws/settle-due` deferred si sales pas final.

Chaque ops request :

- [ ] `dryRun`.
- [ ] `force` seulement si justifié.
- [ ] `reason` obligatoire si force.
- [ ] `maxItems/maxSlots` borné.
- [ ] `daysBack` borné.
- [ ] requestId/idempotency si action sensible.

## P0 — web DTOs

Draw response admin/public doit utiliser summary read model :

- [ ] draw id
- [ ] draw date
- [ ] status
- [ ] scheduledAt
- [ ] cutoffAt
- [ ] channel code/label
- [ ] result slot key/label
- [ ] result summary léger
- [ ] flags : locked, active si admin

Ne pas exposer :

- [ ] raw provider payload.
- [ ] internal debug source_result sauf ops/debug.
- [ ] JPA entity.

## P0 — correction result endpoint

Request :

- [ ] correctedDrawResultId obligatoire.
- [ ] reason obligatoire.
- [ ] idempotencyKey obligatoire.
- [ ] force optionnel, audit obligatoire si true.

Security :

- [ ] permission admin tenant forte.
- [ ] audit action `DRAW_RESULT_CORRECTED` ou équivalent.

## P0 — cancel endpoint

Request :

- [ ] reason obligatoire.
- [ ] force optionnel.

Security/audit :

- [ ] permission admin tenant.
- [ ] audit action `DRAW_CANCELLED`.

## P1 — public draw endpoints

Public home/page model peut consommer :

- [ ] latest results
- [ ] next draws
- [ ] today draws

Contraintes public :

- [ ] Ne pas exposer tenant internals.
- [ ] Ne pas exposer locked/admin flags si inutile.
- [ ] Cache OK court TTL.
- [ ] i18n labels via BFF/page model ou projection.

## Définition de terminé

- Pas d'endpoint legacy vague.
- Admin draw utilise summaries enrichies.
- Ops jobs manuels existent pour debug/scheduler off.
- Actions sensibles sont sécurisées et auditées.
