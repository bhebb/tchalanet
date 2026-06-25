# Design

## Bundle layout

Local fallback translations use this physical layout in each client:

```text
assets/i18n/{locale}/{bundle}.json
```

The canonical bundle order is:

```text
common
domain
component
surface-public
surface-admin
surface-platform
surface-seller-terminal
feature-auth
feature-public
feature-admin
feature-platform
feature-seller-terminal
```

The order is common-to-specific. Later bundles override earlier bundles when the same key exists.

Generic actions and states are shared through `common.*`; product nouns and business
vocabulary are shared through `domain.entity.*`. Feature and surface bundles should not
copy generic labels such as add, save, edit, delete, cancel, view, draw, seller,
seller-terminal, odds, or promotion unless the visible wording intentionally differs.

## Runtime merge

Web loads the configured bundle list for the selected locale, deep-merges objects, and exposes the result to `ngx-translate`. Backend/bootstrap messages are still merged afterwards through `TranslateService.setTranslation(..., true)`.

Mobile loads the same configured bundle list, deep-merges objects, then flattens leaves into dot keys for the existing `I18nBundle.translations` map. Runtime overrides still win.

## Migration references

The initial split preserves existing keys as much as possible. Product-language renames are limited to visible families that need convention alignment, and risky old keys can remain as aliases while backend/PageModel references catch up.
