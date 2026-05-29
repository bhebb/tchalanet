# Spec — Seller

## ADDED Requirements

### Requirement: Seller profile

The system SHALL provide a `core.seller` domain for machann / seller business profiles.

A Seller SHALL have SellerId, tenant id, nullable user id, display name, optional code, and status.

#### Scenario: Seller without user

- GIVEN a tenant admin creates a seller
- WHEN no user is provided
- THEN the seller is created with `user_id = null`

#### Scenario: Seller linked to user later

- GIVEN a seller exists without user
- WHEN tenant admin links a user
- THEN the seller stores the user id
- AND the seller business identity remains unchanged

### Requirement: Seller outlet assignment history

The system SHALL keep historical seller-outlet assignments.

#### Scenario: Seller changes outlet

- GIVEN seller S is assigned to outlet A
- WHEN tenant admin moves S to outlet B
- THEN assignment A is ended
- AND assignment B is opened
- AND existing tickets keep assignment A

### Requirement: Seller sale resolution

The system SHALL resolve the seller for a sale using user, outlet, and session context.

### Requirement: Seller commission policy V1

The system SHALL support simple seller commission policies: NONE, PERCENT, FIXED_PER_TICKET, FIXED_PLUS_PERCENT. Sales SHALL snapshot the applicable policy at sale time.
