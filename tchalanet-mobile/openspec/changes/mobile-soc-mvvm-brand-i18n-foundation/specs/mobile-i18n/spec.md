# Mobile I18n Requirements

## ADDED Requirements

### Requirement: Haitian Creole is default and fallback

The Flutter application SHALL use Haitian Creole (`ht`) as its default and fallback locale.

#### Scenario: No saved or supported device locale exists

- **WHEN** the app starts without a saved supported locale
- **AND** the device locale is unsupported
- **THEN** the active locale is `ht`

### Requirement: All user-visible mobile text is localized

All user-visible mobile text SHALL resolve through the mobile i18n contract or an approved backend message contract.

#### Scenario: A screen displays a stable label

- **WHEN** a screen displays a stable app-owned label, title, hint, action, error, or
  accessibility description
- **THEN** it resolves an i18n key
- **AND** the key exists in `ht`, `fr`, and `en`

#### Scenario: Backend returns a user-visible message

- **WHEN** the backend returns a user-visible business/system message
- **THEN** the app resolves its `messageKey` and params when available
- **AND** it uses the backend fallback only when the key cannot be resolved

### Requirement: Locale changes update the full app

The root Flutter application SHALL be wired to the active locale.

#### Scenario: User changes locale

- **WHEN** the user selects another supported locale
- **THEN** the complete visible application updates without restart
- **AND** the selected locale is persisted
