# Tasks

## Catalog template seed

- [ ] Add/verify `PageModelTemplateView` exposes `code`, `logicalId`, `scope`, `slug`, `schemaVersion`, `schema`, `isDefault`, `isSystem`, `model`.
- [ ] Add `PageModelTemplateCatalog.findDefaultSystemTemplates()`.
- [ ] Add catalog writer/upsert for system templates.
- [ ] Add `PageModelTemplateSeedRunner` with `@Order(10)`.
- [ ] Add static loader for `classpath:/pagemodel/templates/*.template.json`.
- [ ] Upsert templates by `logical_id` or `code`.

## Tenant onboarding

- [ ] Keep `PageModelOnboardingRunner` with `@Order(20)`.
- [ ] Update `PageModelOnboardingService` to iterate catalog default/system templates.
- [ ] Stop using `PageModelType.values()` as seed source.
- [ ] Use `tpl.scope()` and `tpl.slug()` instead of enum values.
- [ ] Continue using `logical_id` as stable PageModel identity.
- [ ] Continue running default tenant seed under `TchContextScope.runStartupTenant`.

## JSON/static pack

- [ ] Move static files under `resources/pagemodel/templates` and `resources/pagemodel/fragments`.
- [ ] Keep template wrappers with metadata + schema + model.
- [ ] Keep fragments typed with `fragment_type` and `schema_version`.
- [ ] Validate fragments/templates at startup.

## JSON ignore / runtime contract

- [ ] Do not reuse seed/template DTOs directly as back-office runtime DTOs.
- [ ] Technical fields may exist in seed/catalog JSON.
- [ ] Runtime PageModel response should remain renderer/product contract.
- [ ] If one DTO is reused internally, use `@JsonIgnore` or mapper methods to avoid leaking technical seed-only fields where not needed.

## Tests

- [ ] Template seed inserts/updates all four templates.
- [ ] Onboarding creates four default PageModel instances for default tenant.
- [ ] Missing template logs warning but does not crash startup.
- [ ] Invalid template model fails validation clearly.
- [ ] Existing PageModel instance is not overwritten silently.
