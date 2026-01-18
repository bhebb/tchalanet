# Typed IDs (Wrappers) Policy

## Rule

- Outside persistence layer: ALWAYS use typed ID wrappers (TenantId, TicketId, UserId, ...)
- Persistence layer only:
  - JPA entities fields use `UUID`
  - Spring Data repositories use `UUID` as ID type
  - Database schema uses `uuid`

## Allowed UUID usage

- JPA entities (`*JpaEntity`)
- Spring Data repositories / JDBC adapters
- Flyway migrations
- Low-level infra integration (Redis keys, external API raw payloads) if needed

## Converters

- Web layer uses Spring Converters:
  - `StringToTenantIdConverter`, etc.
- Controllers MUST accept wrappers in `@PathVariable` and `@RequestParam`.
