# Tasks — align-keycloak-infra-auth-identity-web-mobile

## Infra

- [ ] Update Docker image tags for Postgres, Redis, Traefik, Keycloak.
- [ ] Update `VERSIONS.md` when image versions change.
- [ ] Add healthchecks.
- [ ] Configure Traefik routes for auth/api/app local domains.
- [ ] Document Manjaro setup.

## Keycloak

- [ ] Split realm into base + local/prod overlays.
- [ ] Add `tchalanet-mobile-pos` client.
- [ ] Configure PKCE for web/mobile/swagger.
- [ ] Add prod-ready policies.
- [ ] Add token mappers and token contract tests.
- [ ] Add local dev users.
- [ ] Confirm prod overlay has no demo users.

## Platform identity

- [ ] Normalize current profile path to `/tenant/me/profile`.
- [ ] Merge admin user surfaces into `/admin/identity/users`.
- [ ] Remove tenant id path from tenant-admin user listing.
- [ ] Remove admin bootstrap endpoint.
- [ ] Add Keycloak sync statuses and resync endpoint.
- [ ] Add invitation endpoint.
- [ ] Add validation, audit, Swagger operation docs.

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
