# Specification: admin-api

## ADDED Requirements

### Requirement: Tenant admins can manage promotion rules

The system SHALL provide tenant admin endpoints to list, create, update, activate, deactivate and archive promotion rules.

#### Scenario: Tenant admin creates Christmas multiplier rule

- GIVEN a tenant admin has permission `promotion.admin`
- WHEN they create a rule with `PAYOUT_MULTIPLIER_OVERRIDE`
- THEN the rule SHALL be stored under the current tenant.

### Requirement: Promotion rule writes are audited

All promotion rule writes SHALL create functional audit logs.

#### Scenario: Admin activates rule

- WHEN a tenant admin activates a promotion rule
- THEN audit SHALL record actor, tenant, rule code, action and timestamp.

### Requirement: Admin can test a rule

The admin API SHALL provide a test endpoint returning matched rules, ignored rules and generated effects.

#### Scenario: Test Christmas before noon

- GIVEN a test context with sale time on Dec 25 before noon
- WHEN the test endpoint is called
- THEN the response SHALL show the first prize multiplier effect.

### Requirement: JSON rules are validated before activation

`condition_json` and `effect_json` SHALL be validated before a rule can become active.

#### Scenario: Invalid multiplier

- GIVEN a rule effect has a negative multiplier
- WHEN admin tries to activate the rule
- THEN the system SHALL reject it with `promotion.invalid_effect`.
