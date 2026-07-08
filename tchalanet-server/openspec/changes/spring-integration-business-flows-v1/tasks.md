# Tasks

- [x] Add Spring integration test harness with Testcontainers Postgres.
- [x] Add deterministic business runtime fixtures for tenant/setup/sale flows.
- [ ] Add onboarding default persistence integration test.
- [ ] Add setup readiness transition integration test.
- [x] Add limit-blocked sell integration test.
- [x] Add Maryaj gratis sale integration test.
- [ ] Add receipt print/reprint promotion integration test.
- [ ] Add dedicated Firebase Emulator identity-provider integration test plan or
      tests separately from business runtime tests.
- [x] Document run command and CI marker/profile.

Focused command:

```bash
./mvnw -pl tchalanet-app -am -Dtest=SalesPolicyPromotionSpringIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```
