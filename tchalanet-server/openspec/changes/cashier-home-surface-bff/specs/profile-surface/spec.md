# Spec — profile and surface resolution

## ADDED Requirements

### Requirement: Profile exposes landing surfaces

`GET /tenant/me/profile` SHALL expose available surfaces and preferred landing surface.

#### Scenario: Cashier profile

- **GIVEN** the authenticated user has role `CASHIER`
- **WHEN** the client calls `GET /tenant/me/profile`
- **THEN** the response includes `landing.preferredSurface`
- **AND** `landing.availableSurfaces`
- **AND** user/tenant display metadata.

### Requirement: Surface header is a hint, not authority

The backend SHALL treat `X-Tch-Surface` as a presentation hint only.

#### Scenario: Allowed surface

- **GIVEN** the user has `MOBILE_POS` in `availableSurfaces`
- **WHEN** the client sends `X-Tch-Surface: MOBILE_POS`
- **THEN** the requested surface is accepted.

#### Scenario: Not allowed surface

- **GIVEN** the user does not have `PLATFORM_ADMIN_WEB` in `availableSurfaces`
- **WHEN** the client sends `X-Tch-Surface: PLATFORM_ADMIN_WEB`
- **THEN** the request fails with `403 surface.not_allowed`.

#### Scenario: Missing header

- **GIVEN** the client does not send `X-Tch-Surface`
- **WHEN** the backend resolves the surface
- **THEN** it uses `profile.landing.preferredSurface`.
