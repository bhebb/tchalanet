# Claude/Copilot Context — Tchalanet sales cleanup

Tu travailles dans `tchalanet-server`, projet Java 25 / Spring Boot 4.

Respecte les règles Tchalanet :

- `core.*` = métier, invariants, transactions, commands/queries, ports, events.
- `features.*` = vertical slice / BFF / orchestration UI-oriented. Pas hexagonal par défaut. Pas de JPA/repositories/entities directement.
- `catalog.*` = référentiels/config/read APIs/admin writes simples.
- Controllers minces : mapping, context, CommandBus/QueryBus, security/audit. Pas de logique métier.
- Command handlers : `@UseCase`, `@TchTx`, ports, policies, events `AfterCommit`.
- Query handlers : side-effect free, pas d’audit direct, pas d’events, pas de mutation.
- Pas de `UUID.randomUUID()` direct hors infra/persistence : utiliser `IdGenerator`/typed IDs.
- Pas de `Instant.now()` direct : injecter `Clock`.
- Application queries utilisent `TchPageRequest`, pas Spring `PageRequest`/`Pageable`.
- Public endpoints ne doivent jamais exposer internal IDs : `ticketId`, `drawId`, `tenantId`, `addressId`, terminal UUID.
- Cross-domain SQL join autorisée seulement pour read models/projections optimisées, jamais pour mutations.
- Vues DB = read models uniquement.

## Ne pas faire

- Ne pas déplacer `/sell` en feature.
- Ne pas déplacer `/print` en feature pour l’instant.
- Ne pas faire de `features.ticketverify` hexagonal avec `port/out`/`infra/persistence` si évitable.
- Ne pas accéder à JPA/repositories/entities depuis une feature.
- Ne pas réutiliser `TicketResponse` interne pour public verify.
- Ne pas pointer le QR imprimé vers `/api/v1/...`; il doit pointer vers une page publique `/ticket/{publicCode}`.
- Ne pas accepter `performedBy`/`performedAt` depuis le body pour actions sensibles.
