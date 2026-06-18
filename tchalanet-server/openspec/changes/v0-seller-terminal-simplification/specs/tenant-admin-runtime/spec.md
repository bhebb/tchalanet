# Spec: tenant-admin-runtime

## MODIFIED Requirements

### Requirement: Tenant admin V0 navigation exposes seller-terminal operations

Tenant admin runtime/navigation SHALL present seller-terminal management as the V0 operational seller surface.

#### Scenario: Tenant admin sees seller-terminal management

- **GIVEN** a tenant admin opens the admin runtime
- **WHEN** navigation is assembled
- **THEN** it includes a seller-terminal management destination.

#### Scenario: V0-dead slices are not primary admin destinations

- **GIVEN** payout, ledger, offline sync, autonomy, outlets and sales sessions are parked for V1+
- **WHEN** tenant admin navigation is assembled for V0
- **THEN** those parked slices are not presented as primary active destinations.

### Requirement: Tenant admin seller-terminal endpoints replace old seller/terminal/session endpoints

Backend tenant admin APIs SHALL expose seller-terminal operations through `/admin/seller-terminals` for V0.

The V0 active endpoint family SHALL include:

- `GET /admin/seller-terminals`
- `POST /admin/seller-terminals`
- `GET /admin/seller-terminals/{id}`
- `PATCH /admin/seller-terminals/{id}`
- `POST /admin/seller-terminals/{id}/block`
- `POST /admin/seller-terminals/{id}/unblock`
- `POST /admin/seller-terminals/{id}/reset-access`
- `PATCH /admin/seller-terminals/{id}/commission`
- `PATCH /admin/seller-terminals/{id}/limits`
- `PATCH /admin/seller-terminals/{id}/odds`
- `GET /admin/reports/seller-terminals`

#### Scenario: Admin blocks seller terminal

- **GIVEN** a tenant admin has seller-terminal block permission
- **WHEN** they call `/admin/seller-terminals/{id}/block`
- **THEN** the backend blocks that seller terminal.

#### Scenario: Old seller and terminal endpoints are not required

- **GIVEN** V0 uses seller terminal as the operational unit
- **WHEN** admin runtime is configured
- **THEN** `/admin/sellers` and old-model `/admin/terminals` are not required for selling operations.

#### Scenario: Old session operations are parked

- **GIVEN** V0 no longer requires explicit sales sessions
- **WHEN** admin runtime is configured
- **THEN** session open/close operations are not required for selling.

### Requirement: POS runtime endpoints use seller-terminal actor authority

POS seller-terminal runtime endpoints SHALL require seller-terminal actor authority and SHALL NOT be accessible through admin app-user identity alone.

#### Scenario: POS actor accesses seller-terminal runtime

- **GIVEN** an authenticated actor has authority `ACTOR_SELLER_TERMINAL`
- **WHEN** it calls `/tenant/seller-terminal/me` or `/tenant/seller-terminal/operational-context`
- **THEN** the backend authorizes the request as a POS seller-terminal runtime request.

#### Scenario: Admin actor cannot access POS-only runtime accidentally

- **GIVEN** an authenticated tenant admin lacks authority `ACTOR_SELLER_TERMINAL`
- **WHEN** they call `/tenant/seller-terminal/me`
- **THEN** the backend denies the request before runtime seller-terminal data is returned.
