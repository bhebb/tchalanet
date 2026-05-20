# Application Service Modules — Living Inventory

> This is a living document. It may change without modifying ADR-001. New capabilities should be added here or in a new ADR when they introduce architectural risk.

`<asl>` means the Application Service Layer package name. The final name is pending ADR-001 acceptance.

| Capability | Target module | Public API | Internal responsibilities | Notes |
|---|---|---|---|---|
| Audit | `<asl>.audit` | `api/AuditApi`, audit request/result models, audit events if needed | audit persistence, failure/success logging, Envers bridge if applicable, ops queries | `REQUIRES_NEW` allowed for failure logging |
| Access Control | `<asl>.accesscontrol` | permission check API, role/permission views | role assignments, policy evaluation, permission admin | common.security keeps Spring glue |
| User Context | `<asl>.usercontext` or `<asl>.tenantuser` | user/profile/context API | app user profile, display label, preferences, Keycloak mapping | high fan-in bridge migration expected |
| Tenant Config | `<asl>.tenantconfig` | effective config resolver/admin API | tenant overrides, cache, validation, config update events | catalog.settings may define keys/defaults |
| Tenant Theme | `<asl>.tenanttheme` | effective theme resolver/admin API | tenant overrides, resolution, cache | catalog.theme may define presets |
| Document | `<asl>.document` | document generation/storage API | templates, rendered docs, storage metadata, PDF/QR provider integration | pure render primitives may stay common |
| Communication | `<asl>.communication` | message send API, delivery result | provider adapters, delivery status, templates, retry | consumes core events; must not mutate core |
| Idempotence | `<asl>.idempotence` | idempotency workflow API if needed | records, replay policy, cleanup jobs | annotations/interfaces may stay common |

## Rules for this inventory

- Adding a row does not modify ADR-001.
- Adding a capability with direct dependencies to another `<asl>` capability requires ADR approval.
- Adding a capability that may affect financial/game/regulatory decisions requires core-vs-application-service review.
