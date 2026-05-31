# Task 09 — Seed data: set surface and exposure explicitly for public keys

## Why

After migration, all existing `i18n_override` rows default to `surface = 'INTERNAL'` and all `app_setting` rows default to `exposure = 'INTERNAL'`. Without explicit seed updates, `/public/i18n` and `/public/settings` return empty results.

## i18n seed updates

For any existing seed i18n rows that are intended for public pages, update `surface` explicitly.

Rows to review and update:

```text
Rows for: home page titles, CTAs, error messages used publicly
→ surface = 'PUBLIC_HOME'

Rows for: results page labels
→ surface = 'PUBLIC_RESULTS'

Rows for: ticket check page labels
→ surface = 'PUBLIC_TICKET_CHECK'

Rows for: public-facing error messages
→ surface = 'COMMON_PUBLIC_ERROR'

All others (cashier UI, admin UI, internal labels)
→ keep surface = 'INTERNAL'
```

Edit seed migration(s) in place if pre-go-live. Otherwise add a new `V2xx` seed migration if data is already applied.

## settings seed updates

For any existing seed settings that are intended for public runtime bootstrap, update `exposure` explicitly.

Candidates for `PUBLIC_RUNTIME`:

```text
public.brand_name
public.supported_languages
public.default_locale
public.ticket_check_enabled
public.results_enabled
```

Everything else stays `INTERNAL`.

Edit seed migration(s) in place if pre-go-live. Otherwise add a new `V2xx` seed migration.

## Acceptance criteria

- At least the known public i18n keys have `surface` set to the correct public surface.
- At least the known public runtime settings have `exposure = PUBLIC_RUNTIME`.
- `GET /public/i18n?locale=fr&surface=PUBLIC_HOME` returns a non-empty bundle.
- `GET /public/settings` returns at least the known public runtime settings.
