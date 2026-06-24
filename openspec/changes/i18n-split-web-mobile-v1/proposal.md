# i18n split web/mobile v1

## Why

The web and mobile apps currently load one local translation file per locale. Web has already grown into large mixed bundles, while mobile uses a small flat map. This makes translation ownership, product language, and cross-surface reuse harder to maintain.

## What

- Introduce a shared multi-file local i18n convention for web and mobile.
- Store local fallback bundles under `assets/i18n/{locale}/{bundle}.json`.
- Use nested JSON as the canonical file format.
- Merge local bundles in a configured order before applying backend/bootstrap overrides.
- Add contract checks so all supported locales expose the same final keys.
- Add tooling to inventory declared and referenced keys during the migration.

## Impact

- Web `ngx-translate` receives one merged translation tree per locale.
- Mobile keeps `Map<String, String>` at runtime by flattening the merged nested tree.
- Existing backend i18n contracts and bootstrap override behavior remain unchanged.
- Some old key families may remain as temporary aliases until PageModel/backend references are migrated.

## Non-goals

- No backend contract changes.
- No global rename of every translation key in this change.
- No change to locale defaults: web remains `fr`; mobile remains `ht`.
