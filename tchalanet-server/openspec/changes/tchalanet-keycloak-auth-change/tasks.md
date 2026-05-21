# Tasks — align-keycloak-infra-auth-identity-web-mobile

## Infra

- [x] Update Docker image tags for Postgres, Redis, Traefik, Keycloak. *(Postgres 18.4, Redis 8.6.3, Traefik v3.7.0, Keycloak 26.6.2.)*
- [x] Update `VERSIONS.md` when image versions change.
- [x] Add healthchecks.
- [x] Configure Traefik routes for auth/api/app local domains.
- [x] Document Manjaro setup. *(Skipped per user request; local domain docs are OS-neutral.)*

## Keycloak

- [x] Split realm into base + local/prod overlays.
- [x] Add `tchalanet-mobile-pos` client.
- [x] Configure PKCE for web/mobile/swagger.
- [x] Add prod-ready policies.
- [ ] Add token mappers and token contract tests.
- [x] Add local dev users.
- [x] Confirm prod overlay has no demo users.

## Platform identity

- [x] Normalize current profile path to `/tenant/me/profile`.
- [x] Merge admin user surfaces into `/admin/identity/users`.
- [x] Remove tenant id path from tenant-admin user listing.
- [x] Remove admin bootstrap endpoint.
- [x] Add Keycloak sync statuses and resync endpoint.
- [x] Add invitation endpoint.
- [x] Add validation, audit, Swagger operation docs.

## Web

- [ ] Implement Keycloak auth code + PKCE flow.
- [ ] Add route guards.
- [ ] Load profile after login.
- [ ] Add update profile flow.
- [ ] Add logout flow.

## Mobile

- [ ] Implement Flutter auth code + PKCE flow.
- [ ] Add secure token storage.
- [ ] Add Dio bearer/refresh interceptors.
- [ ] Load/update profile.
- [ ] Add POS dashboard bootstrap.
- [ ] Add offline branchlet placeholder and confirmation flow.

## Tests

- [ ] Docker stack smoke test.
- [ ] Keycloak provider/realm import smoke test.
- [ ] Token claim decode test per user type.
- [ ] Current profile API tests.
- [ ] Admin identity API tests.
- [ ] Web auth smoke test.
- [ ] Mobile auth smoke test.
