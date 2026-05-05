# TODO 03 — Sortir Public Verify en feature

## Objectif

Déplacer la vérification publique du ticket hors `core.sales` vers `features.ticketverify`, sans transformer la feature en domaine hexagonal.

## Décision

```text
features.ticketverify = exposition publique/BFF
core.sales = vérité ticket + read model interne
```

## Core sales doit fournir

```text
GetPublicTicketVerificationRecordQuery
PublicTicketVerificationRecord
PublicTicketVerificationLineRecord
```

Ces modèles sont internes à Spring Boot, pas la response publique finale.

## Feature doit fournir

```text
features.ticketverify.web.TicketVerifyController
features.ticketverify.app.TicketVerifyService
features.ticketverify.mapper.TicketVerifyMapper
features.ticketverify.model.TicketVerifyResponse
features.ticketverify.model.TicketVerifyLineItem
features.ticketverify.model.TicketVerifyOutletView
features.ticketverify.model.TicketVerifyStatus
features.ticketverify.model.TicketVerifyPayoutStatus
```

Pas de repositories/JPA/entities dans la feature.

## P0 — Supprimer l’ancien handler core public-facing

Ancien à remplacer :

```text
core.sales.application.query.handler.VerifyPublicTicketQueryHandler
core.sales.domain.model.TicketVerificationResult
```

- [ ] Ne plus retourner `TicketVerificationResult` domain.
- [ ] Ne plus retourner `null`.
- [ ] Ne plus exposer `ticketId`, `drawId`, `tenantId`, `addressId`, terminal UUID.
- [ ] Ne plus injecter `TerminalReaderPort`, `OutletReaderPort`, `AddressReaderPort` dans un handler public.
- [ ] Ne plus calculer payoutStatus avec `potentialTotal`.
- [ ] Ne plus utiliser `Instant.now()` direct.
- [ ] Ne plus `catch(Exception ignored)`.

## P0 — Core sales record interne

Créer par exemple :

```java
public record PublicTicketVerificationRecord(
    TenantId tenantId,
    String publicCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    Instant createdAt,
    BigDecimal totalAmount,
    BigDecimal winningAmount,
    String outletName,
    String outletCity,
    String outletCountry,
    List<PublicTicketVerificationLineRecord> lines
) {}
```

- [ ] Le `tenantId` peut exister dans le record interne pour résoudre settings/visibility.
- [ ] Il ne doit jamais sortir dans la response publique.
- [ ] Adapter core peut lire `v_ticket_print` ou projection dédiée plus tard.

## P0 — Feature service

`TicketVerifyService` :

- [ ] Normalise publicCode.
- [ ] Si code vide/malformed => `INVALID_CODE`.
- [ ] Appelle `QueryBus` -> `GetPublicTicketVerificationRecordQuery`.
- [ ] Si absent => `NOT_FOUND`.
- [ ] Résout visibilité publique, fallback 14 jours.
- [ ] Mappe vers response publique.
- [ ] N’expose aucun internal ID.

## P0 — Statuts publics

```text
TicketVerifyStatus:
  VALID
  INVALID_CODE
  NOT_FOUND
  EXPIRED
  VOID

TicketVerifyPayoutStatus:
  PENDING_DRAW
  WON_UNCLAIMED
  WON_PAID
  LOST
  VOID
  EXPIRED
  UNKNOWN
```

Matrice :

```text
saleStatus VOID/REJECTED        -> VOID
expired                         -> EXPIRED
resultStatus NOT_RESULTED       -> PENDING_DRAW
resultStatus LOST               -> LOST
resultStatus WON + UNSETTLED    -> WON_UNCLAIMED
resultStatus WON + SETTLED      -> WON_PAID
otherwise                       -> UNKNOWN
```

## P0 — Controller public

- [ ] Route page/API publique selon décision produit : `/ticket/{code}` ou `/api/v1/public/tickets/{code}/verify` selon stack web/API.
- [ ] Ajouter noindex header.
- [ ] Ajouter `Cache-Control: no-store`.
- [ ] Ajouter rate-limit au controller/filter/gateway.
- [ ] Réponse publique masquée.

## Tests P0

- [ ] null/blank code -> `INVALID_CODE`.
- [ ] malformed code -> `INVALID_CODE`.
- [ ] unknown -> `NOT_FOUND`.
- [ ] expired -> `EXPIRED`, sans lignes/montants/outlet si politique choisie.
- [ ] NOT_RESULTED -> `PENDING_DRAW`.
- [ ] WON + UNSETTLED -> `WON_UNCLAIMED`.
- [ ] WON + SETTLED -> `WON_PAID`.
- [ ] LOST -> `LOST`.
- [ ] VOID/REJECTED -> `VOID`.
- [ ] Response ne contient pas internal IDs.
