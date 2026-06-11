# Public Shell V1

## ADDED Requirements

### Requirement: Public routes share a routed shell

Public routes SHALL render inside a single public shell that owns the header, routed content outlet,
and footer.

#### Scenario: Navigate between public pages

- **WHEN** a user navigates between routes under `/public`
- **THEN** the routed page renders inside `TchPublicShellComponent`
- **AND** the shell header and footer remain outside the routed page component

### Requirement: Public language selection uses runtime metadata

The public shell SHALL hydrate available languages from resolved PageModel runtime metadata when the
backend provides it and SHALL remain usable when that metadata is absent.

#### Scenario: Runtime provides supported languages

- **WHEN** a public runtime response includes `meta.currentLang` and `meta.supportedLangs`
- **THEN** the public shell updates the language store from that metadata

#### Scenario: Runtime omits supported languages

- **WHEN** a public runtime response omits `meta.supportedLangs`
- **THEN** the shell keeps the existing frontend language configuration

### Requirement: Public shell navigation is accessible

The public header SHALL provide accessible mobile navigation controls and the language switcher SHALL
use an accessible Material menu.

#### Scenario: Use the mobile public header

- **WHEN** a user opens or closes the mobile navigation
- **THEN** the burger control exposes the translated action label
- **AND** the navigation remains keyboard operable
