# Prompt task 03 — Feature ticketverify

Déplace la vérification publique vers `features.ticketverify`.

Core sales doit fournir un read model interne via QueryBus :

- `GetPublicTicketVerificationRecordQuery`
- `PublicTicketVerificationRecord`
- `PublicTicketVerificationLineRecord`

Feature :

- `features.ticketverify.web.TicketVerifyController`
- `features.ticketverify.app.TicketVerifyService`
- `features.ticketverify.mapper.TicketVerifyMapper`
- response publique masquée
- statuts publics
- noindex/cache-control/rate-limit hook

Interdictions :

- Pas de JPA/repository/entity dans feature.
- Pas d’internal IDs dans response.
- Ne pas retourner null.
