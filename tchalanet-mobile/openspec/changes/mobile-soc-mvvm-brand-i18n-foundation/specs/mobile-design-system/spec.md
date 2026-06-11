# Mobile Design System Requirements

## ADDED Requirements

### Requirement: Default mobile brand aligns with Tchalanet web

The default Flutter Material 3 theme SHALL use the Tchalanet web brand roles.

#### Scenario: App starts without a runtime theme response

- **WHEN** the mobile app starts before a backend theme is available
- **THEN** Material 3 primary is based on deep navy `#1A1B4B`
- **AND** the brand accent is Material 3 tertiary gold `#FECB00`
- **AND** the app renders with the validated local default token set

### Requirement: Features consume semantic tokens

Feature Views SHALL consume Material 3 roles and typed mobile design tokens.

#### Scenario: A feature needs a color or visual metric

- **WHEN** a feature needs color, spacing, radius, elevation, or typography
- **THEN** it uses a semantic Material 3 role or approved typed Tchalanet token
- **AND** it does not introduce a repeated hardcoded brand value

### Requirement: Web parity is adapted to mobile

Mobile SHALL preserve Tchalanet brand coherence without copying unsuitable web layouts.

#### Scenario: A Stitch or web screen is used as a visual reference

- **WHEN** a mobile screen is derived from a Stitch or web reference
- **THEN** its visual decisions are expressed through shared tokens/components
- **AND** POS touch, accessibility, adaptive layout, and critical-action rules take
  precedence over pixel parity
