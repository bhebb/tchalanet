# Verify Ticket — Flow public

> Vérification publique d'un ticket par son `publicCode` : statut, gain potentiel, lignes, outlet émetteur (masqué). Aussi : génération PNG QR code public. Endpoints sans authentification, avec headers `X-Robots-Tag: noindex, nofollow`.

---

## Vue d'ensemble (1 query, 0 mutation)

```
Public (URL/QR)        Backend                              Sources lues
    │                     │                                    │
    ▼                     ▼                                    ▼
┌──────────┐    ┌─────────────────────┐         ┌────────────────────────────┐
│  GET     │ →  │  Verify             │ ─────→ │ ticket (RLS bypass public) │
│ /public/ │    │  PublicTicket       │ ─────→ │ catalog.settings           │
│ tickets/ │    │  QueryHandler       │ ─────→ │ core.terminal / core.outlet /   │
│ verify/  │    │                     │         │ core.address (best-effort) │
│ {code}   │    │                     │         └────────────────────────────┘
└──────────┘    └─────────────────────┘
```

| Phase             | Domaine pivot                                    | Action                                                                                 |
| ----------------- | ------------------------------------------------ | -------------------------------------------------------------------------------------- |
| **Lookup ticket** | `core.sales`                                     | `TicketReaderPort.findByPublicCode(code)` (normalise uppercase + strip `-` ` `)        |
| **Visibility**    | `catalog.settings`                               | `SettingsCatalog.resolve("ticket.verification:public_visibility_days")` (default 14 j) |
| **Project view**  | `core.sales`                                     | Construction `TicketVerificationResult` (visible ou minimal expired)                   |
| **Enrich outlet** | `core.terminal` / `core.outlet` / `core.address` | Best-effort lookup (catch global) ; address masquée                                    |
| **Render**        | controller                                       | `ResponseEntity<?>` raw + headers `noindex` + `Cache-Control: no-store`                |

---

## Domaines impliqués

| Domaine            | Type   | Rôle                                                          |
| ------------------ | ------ | ------------------------------------------------------------- |
| `core.sales`       | tenant | Pivot : lookup `Ticket` par `publicCode`, construction du DTO |
| `catalog.settings` | tenant | Visibilité publique paramétrable (default 14 j)               |
| `core.terminal`    | tenant | `TerminalReaderPort.findById` → `outletId`                    |
| `core.outlet`      | tenant | `OutletReaderPort.findById` → `name`, `addressId`             |
| `core.address`     | tenant | `AddressReaderPort.findById` → `Address` masqué               |

---

## Vocabulaire métier

| Terme            | Sens court                                                                 | Source of truth                         |
| ---------------- | -------------------------------------------------------------------------- | --------------------------------------- |
| **publicCode**   | Code Crockford Base32 12 chars (globalement unique)                        | `core/sales/DOMAIN_SALES.md`            |
| **Visibility**   | Fenêtre temporelle pendant laquelle un ticket est consultable publiquement | `catalog/settings/CATALOG_SETTINGS.md`   |
| **payoutStatus** | `POTENTIAL_WIN` / `NO_PAYOUT` / `EXPIRED` (⚠ basé sur `potentialPayout`)   | `TicketVerificationResult.payoutStatus` |

---

## Pipeline détaillé — `GET /public/tickets/verify/{publicCode}`

```
GET /public/tickets/verify/{publicCode}
   │
   ▼
PublicTicketController.verify(publicCode)
   │  ⚠ Aucun rate-limiting effectif
   │  ⚠ Retourne ResponseEntity<?> raw (non conforme ApiResponse<T>)
   │
   ▼
QueryBus.send(VerifyPublicTicketQuery(publicCode, now))
   │
   ▼
VerifyPublicTicketQueryHandler
   │
   ├── Normalisation : trim → uppercase Locale.ROOT → remove '-' et ' '
   │
   ├── TicketReaderPort.findByPublicCode(code)
   │     └── @EntityGraph "lines" sur SpringTicketJpaRepository
   │     └── Si absent → null → controller renvoie 404
   │
   ├── resolveVisibilityDays(tenantId)
   │     └── SettingsCatalog.resolve(forTenant(tenantId, ["ticket.verification"]))
   │           └── lookup setting "public_visibility_days"
   │           └── Exception ou absence → default 14 jours (silencieux)
   │
   ├── isVisible(ticket, now, visibilityDays)
   │     └── ticket.createdAt + visibilityDays > now ?
   │
   ├── Si NON visible → return TicketVerificationResult minimal :
   │     ├── ticketId, publicCode, terminalMasked, createdAt
   │     ├── saleStatus / resultStatus / settlementStatus = null
   │     ├── drawId = null, totalAmount = null, potentialTotalPayout = null
   │     ├── outletName = null, outletAddress = null, lines = []
   │     └── payoutStatus = "EXPIRED"
   │
   └── Si visible → toVisibleResult(ticket) :
         ├── lines = ticket.lines mappées { gameCode.name(), selection, stake, potentialPayout }
         ├── potentialTotal = sum(potentialPayout)
         ├── payoutStatus = potentialTotal > 0 ? "POTENTIAL_WIN" : "NO_PAYOUT"
         │     ⚠ Ne reflète PAS resultStatus / settlementStatus réels
         ├── try { best-effort lookup outlet } catch (Exception ignored) { /* silencieux */ }
         │     ├── TerminalReaderPort.findById(tenantId, terminalId) → outletId
         │     ├── OutletReaderPort.findById(outletId) → name + addressId
         │     └── AddressReaderPort.findById(tenantId, addressId) → Address
         ├── maskAddress(address) :
         │     └── garde city + country, efface line1/line2/region/postalCode/normalizedKey
         │     ⚠ ne masque pas address.id ni address.tenantId
         └── return TicketVerificationResult complet (statuts + masques inclus)
   │
   ▼
ResponseEntity.ok()
   .header("X-Robots-Tag", "noindex, nofollow")
   .header("Cache-Control", "no-store")
   .body(result)
```

---

## Pipeline annexe — `GET /public/tickets/qr/{publicCode}.png?size=280`

```
GET /public/tickets/qr/{publicCode}.png
   │
   ▼
PublicTicketController.qrPng(publicCode, size=280)
   │
   ▼
QueryBus.send(GetTicketQrPngByPublicCodeQuery(publicCode, size))
   │
   ▼
GetTicketQrPngByPublicCodeQueryHandler
   │
   ├── QrPayloadBuilder.build(publicCode) → URL/payload signé
   ├── QrRenderer.render(payload, size) → byte[] PNG
   │     └── catch IllegalArgumentException → 404
   │
   ▼
ResponseEntity.ok()
   .header("X-Robots-Tag", "noindex, nofollow")
   .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
   .contentType(MediaType.IMAGE_PNG)
   .body(png)
```

---

## Réponse — `TicketVerificationResult`

```jsonc
{
  "ticketId": "uuid", // ⚠ UUID interne en clair
  "publicCode": "ABC123XYZ456", // 12 chars Crockford
  "saleStatus": "SOLD|PENDING_APPROVAL|VOID|REJECTED|null",
  "resultStatus": "NOT_RESULTED|WON|LOST|OVERRIDDEN|null",
  "settlementStatus": "UNSETTLED|SETTLED|null",
  "drawId": "uuid|null", // ⚠ UUID interne
  "terminalMasked": "abcd1234…", // 8 premiers chars du UUID + ellipsis
  "createdAt": "2026-04-26T15:00:00Z",
  "totalAmount": 100.0,
  "potentialTotalPayout": 5000.0,
  "payoutStatus": "POTENTIAL_WIN", // ⚠ basé sur potentialPayout, pas winningAmount
  "outletName": "Outlet ABC",
  "outletAddress": {
    "id": "uuid", // ⚠ NON masqué
    "tenantId": "uuid", // ⚠ NON masqué
    "city": "Port-au-Prince",
    "country": "HT",
    "line1": null,
    "line2": null,
    "region": null,
    "postalCode": null
  },
  "lines": [
    { "gameCode": "HT_BOLET", "selection": "23,45", "stake": 50.0, "potentialPayout": 2500.0 }
  ]
}
```

> Mode `EXPIRED` (au-delà de la fenêtre `public_visibility_days`) : `saleStatus`, `resultStatus`, `settlementStatus`, `drawId`, `totalAmount`, `potentialTotalPayout`, `outletName`, `outletAddress` à `null` ; `lines = []` ; `payoutStatus = "EXPIRED"`.

---

## Décisions / branches

### Visibilité

- Configurable par tenant via `catalog.settings` clé `ticket.verification:public_visibility_days`.
- Default : 14 jours (codé en dur dans le handler en cas d'exception).
- Au-delà : `payoutStatus = "EXPIRED"` ; tous les champs sensibles sont remplacés par `null` (mais `ticketId`, `publicCode`, `terminalMasked`, `createdAt` restent exposés).

### Cache

- Backend : `Cache-Control: no-store` sur `verify` (ne PAS cacher en CDN).
- QR PNG : `Cache-Control: public, max-age=3600` (cacheable 1h).

### SEO

- `X-Robots-Tag: noindex, nofollow` sur les deux endpoints.

### Rate-limiting

- **Non implémenté** malgré commentaire d'intention dans le handler.

---

## Sécurité — surfaces d'exposition publique

| Donnée                       | Exposée ?     | Notes                                               |
| ---------------------------- | ------------- | --------------------------------------------------- |
| `ticketId` (UUID)            | ✅ en clair   | UUID interne                                        |
| `publicCode`                 | ✅ en clair   | Code public conçu pour ce partage                   |
| `drawId` (UUID)              | ✅ en clair   | UUID interne                                        |
| `terminalMasked`             | ✅ partiel    | 8 premiers chars du UUID + `…`                      |
| `outletAddress.id`           | ✅ en clair   | ⚠ Devrait être masqué                               |
| `outletAddress.tenantId`     | ✅ en clair   | ⚠ Fuite identifiant tenant                          |
| `outletAddress.city/country` | ✅ en clair   | Volontaire (city + country uniquement)              |
| `outletName`                 | ✅ en clair   | Raison sociale outlet                               |
| `lines[].selection`          | ✅ en clair   | Mises (selection)                                   |
| `lines[].stake`              | ✅ en clair   | Montant misé                                        |
| `lines[].potentialPayout`    | ✅ en clair   | Gain potentiel pré-tirage                           |
| `winningAmount` réel         | ❌ non exposé | Le handler n'expose pas `winningAmount` post-tirage |

---

## Cross-apps

### Mobile (POS / customer scan)

- Cashier scanne le QR du ticket → ouvre `https://app.tchalanet.com/v/{publicCode}` qui frontend appelle `/public/tickets/verify/{publicCode}`.
- Affiche statut + lignes + outlet émetteur + payoutStatus.

### Web (public landing)

- Page `/v/{publicCode}` (frontend Angular `public-verify-page`).
- Composants : `TicketVerifyCard`, `TicketLinesTable`, `OutletInfoBlock`.
- i18n : `verify.*`, `ticket.*`.

### API publique

- `GET /api/v1/public/tickets/verify/{publicCode}` — vérification.
- `GET /api/v1/public/tickets/qr/{publicCode}.png?size=280` — QR PNG (cacheable 1h).

---

## Source of truth backend

> Cette page est une **vue fonctionnelle cross-apps**. La source de vérité technique vit près du code.

- Backend `core.sales` : `99-links/_ref/server/core/sales/DOMAIN_SALES.md`
- Backend `catalog.settings` : `99-links/_ref/server/catalog/settings/CATALOG_SETTINGS.md`
- Backend `core.outlet` : `99-links/_ref/server/core/outlet/DOMAIN_OUTLET.md`
- Backend `core.terminal` : `99-links/_ref/server/core/terminal/DOMAIN_TERMINAL.md`
- Backend `core.address` : `99-links/_ref/server/core/address/DOMAIN_ADDRESS.md`
- Audit : `99-links/_ref/server/docs/audit/2026-04-26-sales-pipeline-audit.md`

> En cas d'incohérence entre cette page et les `DOMAIN_*.md` backend, **les docs backend font foi**.

---

## Liens

- Architecture backend : `tchalanet-server/docs/ARCHITECTURE.md`
- Functional domains : `docs/02-functional/domains/sales.md`, `settings.md`
- Flow vente : `docs/02-functional/flows/sell-ticket.md`
