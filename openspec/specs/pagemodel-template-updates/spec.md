# pagemodel-template-updates Specification

## Purpose

TBD - created by archiving change add-pagemodel-template-update-workflow. Update Purpose after archive.

## Requirements

### Requirement: Template update event must not silently mutate published PageModels

The system SHALL NOT directly mutate published PageModels as an automatic side effect of `PageModelTemplateUpdatedEvent`.

#### Scenario: Published PageModel affected by template update

- **GIVEN** a tenant has a published PageModel based on a template
- **WHEN** the template is updated
- **THEN** the published PageModel remains unchanged
- **AND** an actionable notification is created for tenant admins

### Requirement: Template update event creates actionable notifications

The system SHALL create actionable notifications for affected tenant admins when a template update may affect tenant PageModels.

#### Scenario: Template update affects tenant page

- **WHEN** `PageModelTemplateUpdatedEvent` is handled
- **THEN** the system identifies affected tenants/page models
- **AND** creates a deduped `ACTION_REQUIRED` notification with category `PAGE_MODEL`

### Requirement: Backend recalculates update state during review/action

The system SHALL recalculate PageModel/template update state when the admin previews or applies an action.

Notification payload SHALL NOT be treated as the source of truth for applying changes.

#### Scenario: Template changed again after notification

- **GIVEN** a notification was created for schema version 3
- **AND** the template is now at schema version 4
- **WHEN** the admin opens preview
- **THEN** the backend uses the current template/page state
- **AND** reports whether the original notification is stale

### Requirement: Support compatibility levels

The system SHALL classify template changes using compatibility levels:

- `PATCH`
- `MINOR`
- `MAJOR`

#### Scenario: Major compatibility change

- **WHEN** a template update is classified as `MAJOR`
- **THEN** automatic merge SHALL be disabled
- **AND** the review result SHALL indicate `REQUIRES_MIGRATION` or explicit replace

### Requirement: Support merge preview

The system SHALL provide a preview/diff before applying template changes to an existing PageModel.

The preview SHALL include:

- additions,
- removals,
- modifications,
- conflicts,
- recommended action.

#### Scenario: Tenant admin previews update

- **WHEN** tenant admin opens update review
- **THEN** the system returns a diff summary
- **AND** indicates whether merge is safe, conflictual, draft-recommended, replace-only, or migration-required

### Requirement: Support merge action

The system SHALL support merging template changes into an existing PageModel when compatible.

#### Scenario: Safe merge

- **GIVEN** there are no conflicts
- **WHEN** tenant admin accepts merge
- **THEN** the PageModel is updated or a draft is created according to publication status and request mode
- **AND** the decision is audited

### Requirement: Support draft creation

The system SHALL support creating a draft from a template update without changing the published PageModel.

#### Scenario: Published page draft

- **GIVEN** a PageModel is published
- **WHEN** tenant admin chooses create draft
- **THEN** the system creates a draft PageModel using the merged template result
- **AND** the published PageModel remains unchanged

### Requirement: Support replace all

The system SHALL support replacing an existing PageModel with the new template model only by explicit admin action.

#### Scenario: Replace published page

- **GIVEN** a PageModel is published
- **WHEN** tenant admin explicitly chooses replace all
- **THEN** the system creates a backup/draft snapshot first
- **AND** replaces the target PageModel according to publication rules
- **AND** audits the decision

### Requirement: Support ignore

The system SHALL allow tenant admins to ignore a template update.

#### Scenario: Ignore update

- **WHEN** tenant admin chooses ignore
- **THEN** the system records or audits the ignore decision
- **AND** the PageModel remains unchanged
- **AND** related actionable notification is marked handled/read/archived according to notification rules

### Requirement: Keep PageModels functional during pending updates

The system SHALL keep existing PageModels renderable while template update review is pending.

#### Scenario: Update pending

- **GIVEN** a template update notification is pending
- **WHEN** public or private page rendering requests the PageModel
- **THEN** the current valid PageModel is returned
- **AND** rendering is not blocked by pending update workflow
