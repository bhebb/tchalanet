# Change: pagemodel-template-seeding-v4

## Why

PageModel has two distinct levels that were previously blurred:

1. `PageModelTemplate` in catalog: system/default source definition used for seed and validation.
2. `PageModelInstance` in core: tenant-scoped published instance created from a template.

The current onboarding service loops over `PageModelType.values()` and then finds a template by logicalId.
This makes Java enum the seed driver and hides important template columns such as scope and slug.

## What changes

- Seed static PageModelTemplate JSON files into `catalog.pagemodeltemplate`.
- Then create tenant PageModel instances from catalog templates.
- Move default/onboarding iteration from `PageModelType.values()` to `templateCatalog.findDefaultSystemTemplates()`.
- Use template columns: `logical_id`, `scope`, `slug`, `schema_version`, `schema`, `model`.
- Keep `logical_id` as the stable identity.
- Keep technical seed fields in template/catalog JSON, not necessarily in runtime back-office PageModel responses.

## Non-goals

- Do not build a full CMS.
- Do not let frontend choose arbitrary private PageModel by role.
- Do not expose seed/template technical fields as back-office UI contract.
