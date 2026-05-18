# limitpolicy Specification delta for offlinesync

## ADDED Requirements

### Requirement: provide offline limit policy

The limit policy module SHALL expose a read-only query returning offline limits applicable to a tenant and operational context.

#### Scenario: policy returned

- **GIVEN** a tenant has offline mode enabled
- **WHEN** offlinesync asks `GetOfflineLimitPolicyQuery`
- **THEN** limitpolicy returns max grant duration, max ticket count, max total amount, and sync accepted extension

#### Scenario: offline disabled

- **GIVEN** a tenant has offline mode disabled
- **WHEN** offlinesync asks `GetOfflineLimitPolicyQuery`
- **THEN** the response indicates offline is not allowed
