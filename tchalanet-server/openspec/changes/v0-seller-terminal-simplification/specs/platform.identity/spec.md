# Spec: platform.identity

## ADDED Requirements

### Requirement: Platform identity exposes seller-terminal lookup SPI

`platform.identity` SHALL define a public seller-terminal lookup SPI used by authentication/bootstrap code, and the SPI SHALL be implemented by `core.sellerterminal`.

The public API SHALL include:

- `platform.identity.api.SellerTerminalIdentityLookup`
- `platform.identity.api.model.SellerTerminalIdentityBootstrapView`
- `platform.identity.api.model.SellerTerminalBootstrapStatus`

#### Scenario: Bootstrap depends on public seller-terminal lookup

- **GIVEN** `UserBootstrapFilterImpl` resolves a POS actor
- **WHEN** it needs seller-terminal identity data
- **THEN** it calls `SellerTerminalIdentityLookup`
- **AND** it does not depend on `features.cashier` or `core.sellerterminal.internal` types.

#### Scenario: Core sellerterminal implements lookup

- **GIVEN** the seller-terminal lookup SPI is registered
- **WHEN** Spring wires authentication bootstrap
- **THEN** the implementation is provided by `core.sellerterminal.internal.infra.identity.CoreSellerTerminalIdentityLookupAdapter`.

### Requirement: POS bootstrap is separated from admin app-user bootstrap

Seller-terminal POS authentication SHALL use seller-terminal identity lookup, while admin authentication SHALL use app-user lookup.

#### Scenario: POS client resolves seller terminal

- **GIVEN** a verified external identity
- **AND** the request declares `X-Tch-Client-Type=POS`
- **WHEN** bootstrap runs
- **THEN** it resolves a seller terminal through `SellerTerminalIdentityLookup`
- **AND** the resulting actor type is `SELLER_TERMINAL`
- **AND** the actor has authority `ACTOR_SELLER_TERMINAL`.

#### Scenario: POS client without mapping fails without app-user fallback

- **GIVEN** a verified external identity
- **AND** the request declares `X-Tch-Client-Type=POS`
- **AND** no active seller-terminal mapping exists
- **WHEN** bootstrap runs
- **THEN** the request fails with `terminal.external_identity_not_linked` or `terminal.not_active`
- **AND** bootstrap does not attempt app-user lookup.

#### Scenario: Admin client without app user fails without seller-terminal fallback

- **GIVEN** a verified external identity
- **AND** the request is not a POS client
- **AND** no active app-user mapping exists
- **WHEN** bootstrap runs
- **THEN** the request fails with `external_identity.not_linked` or `user.not_active`
- **AND** bootstrap does not attempt seller-terminal lookup.

#### Scenario: Missing verified external identity is rejected

- **GIVEN** an authentication request lacks a verified issuer and subject
- **WHEN** bootstrap runs
- **THEN** the request fails with `external_identity.missing_verified_identity`.

### Requirement: Seller-terminal bootstrap enforces active status

Only seller terminals with status `ACTIVE` SHALL bootstrap as POS actors.

#### Scenario: Active terminal is bootstrapped

- **GIVEN** a verified external identity maps to a seller terminal with status `ACTIVE`
- **WHEN** POS bootstrap runs
- **THEN** it creates `BootstrappedActor.sellerTerminal(...)` with seller terminal id, tenant id, provider, issuer, subject and actor type `SELLER_TERMINAL`.

#### Scenario: Forbidden statuses do not bootstrap

- **GIVEN** a verified external identity maps to a seller terminal with status `BLOCKED`, `SUSPENDED`, `DISABLED` or `DELETED`
- **WHEN** POS bootstrap runs
- **THEN** bootstrap fails with `terminal.not_active`
- **AND** no seller-terminal actor is attached to request context.
