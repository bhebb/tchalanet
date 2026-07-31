# admin-subscription-resilience Specification

## ADDED Requirements

### Requirement: Subscription errors follow page and action ownership

The subscription admin page SHALL treat subscription loading as page-owned and renew, cancel,
suspend, and resume failures as Actions-section-owned normalized errors.

#### Scenario: Subscription load failure

- **GIVEN** the subscription request fails
- **WHEN** the page loads
- **THEN** the page renders a normalized blocking error with retry.

#### Scenario: Subscription action failure

- **GIVEN** a subscription action is submitted
- **WHEN** the action fails
- **THEN** the Actions section displays normalized feedback
- **AND** the page shell does not display a duplicate generic error.
