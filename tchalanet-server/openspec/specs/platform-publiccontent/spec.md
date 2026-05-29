# platform-publiccontent Specification

## Purpose

`platform.publiccontent` is a transversal platform capability for editorial, public, and
network content managed centrally. It is separate from `platform.notification` (which handles
transactional/action-required messages). Consumers access it through `PublicContentApi`.

## Requirements

### Requirement: Public content platform capability

The system SHALL provide a `platform.publiccontent` capability for editorial/public/network content managed by the platform.

#### Scenario: Public home asks for latest public news

- **WHEN** the public home PageModel provider requests latest public news
- **THEN** it SHALL call `platform.publiccontent.api.PublicContentApi`
- **AND** it SHALL NOT depend on `features.news`.

#### Scenario: Platform admin publishes internal network content

- **WHEN** a SUPER_ADMIN creates a public content item
- **THEN** the item SHALL be stored in the internal public content source
- **AND** it SHALL support `DRAFT`, `PUBLISHED`, and `ARCHIVED` status
- **AND** it SHALL support one or more display surfaces.

#### Scenario: External RSS is aggregated

- **WHEN** public content is aggregated for `PUBLIC_HOME`
- **THEN** the system SHALL include published internal content for `PUBLIC_HOME`
- **AND** it MAY include external RSS items from the configured provider
- **AND** hidden items SHALL be excluded.

### Requirement: Public content is not notification

The system SHALL keep `platform.publiccontent` separate from `platform.notification`.

#### Scenario: Content is editorial and not action-required

- **WHEN** a message is a general announcement, RSS item, homepage message, or network communication
- **THEN** it SHALL be represented as public content.

#### Scenario: Message is transactional or action-required

- **WHEN** a message is event-triggered, targeted to a user/tenant/role, has delivery/read state, or requires operational action
- **THEN** it SHALL be represented as a notification, not public content.

### Requirement: Surface targeting

Internal public content SHALL support display surface targeting.

#### Scenario: Content targets tenant admin dashboard

- **GIVEN** an internal content item with surface `TENANT_ADMIN_DASHBOARD`
- **WHEN** tenant admin dashboard requests content
- **THEN** the item SHALL be eligible for display if status and publication window allow it.

#### Scenario: Content targets only public home

- **GIVEN** an internal content item with surface `PUBLIC_HOME`
- **WHEN** tenant admin dashboard requests content
- **THEN** the item SHALL NOT be returned unless it also targets `TENANT_ADMIN_DASHBOARD` or an equivalent shared admin surface.

### Requirement: User personalization deferred

The system SHALL NOT implement per-user public content hiding preferences in V1.

#### Scenario: V1 dashboard loads content

- **WHEN** a dashboard loads public content
- **THEN** the platform SHALL return content according to status, publication window, hidden overlay, and surface targeting
- **AND** user-specific preferences SHALL be ignored because they are V2.
