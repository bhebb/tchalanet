# i18n local bundles

## ADDED Requirements

### Requirement: Clients load local fallback translations from ordered locale bundle files

Web and mobile clients MUST store local fallback translations under `assets/i18n/{locale}/{bundle}.json` and merge the configured bundle list in common-to-specific order before applying runtime/backend overrides.

#### Scenario: Web loads a locale

- **GIVEN** the active web locale is `fr`
- **WHEN** the translate loader requests local fallback translations
- **THEN** it loads the configured bundle files from `/assets/i18n/fr/{bundle}.json`
- **AND** it deep-merges the files in configured order into one translation tree
- **AND** runtime bootstrap translations still override local values afterwards.

#### Scenario: Mobile loads a locale

- **GIVEN** the active mobile locale is `ht`
- **WHEN** the i18n repository loads local fallback translations
- **THEN** it loads the configured bundle files from `assets/i18n/ht/{bundle}.json`
- **AND** it deep-merges the files in configured order
- **AND** it flattens nested leaves into dot keys for the existing mobile translation map
- **AND** runtime bootstrap overrides still win afterwards.

### Requirement: Supported locales expose the same local key contract

Each supported locale MUST provide the same required bundle filenames and final translation keys after local merge.

#### Scenario: Locale bundle contract is checked

- **GIVEN** `fr`, `en`, and `ht` are supported locales
- **WHEN** i18n contract validation runs
- **THEN** each locale has the configured bundle files
- **AND** each locale exposes the same final merged key set.
