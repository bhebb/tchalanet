# Design — Home Surface BFF + PageModel Split

## 1. Boundary

Use both mechanisms, but for different jobs.

```text
PageModel
  = layout/configuration/visibility for rich pages

Home BFF
  = real-time operational state for a concrete surface
```

POS state is dynamic and action-critical:

```text
operationalContext.ready
operationalContext.trusted
session.open
primaryDraw.status
primaryDraw.cutoffAt
actions.canSell
requiredStep
```

These fields must come from feature/core queries, not from PageModel.

## 2. Surface header contract

### Header

```http
X-Tch-Surface: MOBILE_POS
```

### Allowed values

```java
public enum ClientSurface {
    MOBILE_POS,
    CASHIER_WEB,
    TENANT_ADMIN_WEB,
    PLATFORM_ADMIN_WEB
}
```

### Resolution rule

```text
requestedSurface = header X-Tch-Surface if present
else profile.preferredSurface
else role-based default
```

### Security rule

The surface is not trusted for authorization.

```text
requestedSurface must be included in profile.availableSurfaces.
```

Examples:

```text
CASHIER may request MOBILE_POS or CASHIER_WEB if allowed.
TENANT_ADMIN may request TENANT_ADMIN_WEB and optionally MOBILE_POS if POS mode selected.
SUPER_ADMIN may request PLATFORM_ADMIN_WEB.
```

## 3. Profile endpoint

```http
GET /tenant/me/profile
```

Example:

```json
{
  "user": {
    "userId": "usr_...",
    "displayName": "Agent",
    "username": "agent01",
    "roles": ["CASHIER"],
    "locale": "fr",
    "timezone": "America/Port-au-Prince"
  },
  "tenant": {
    "tenantId": "tenant_...",
    "name": "Demo Tenant",
    "code": "demo",
    "currency": "HTG"
  },
  "landing": {
    "preferredSurface": "MOBILE_POS",
    "availableSurfaces": ["MOBILE_POS", "CASHIER_WEB"]
  },
  "capabilities": [
    "cashier.sell",
    "cashier.print",
    "cashier.send",
    "cashier.cancel"
  ],
  "profileActions": {
    "canEditDisplayName": true,
    "canEditLocale": true,
    "canEditTimezone": false,
    "canChangePassword": true
  }
}
```

## 4. Mobile POS home endpoint

```http
GET /tenant/cashier/home
X-Tch-Surface: MOBILE_POS
```

This is the primary mobile/POS endpoint.

### Ready state

```json
{
  "surface": "MOBILE_POS",
  "version": "home.v1",
  "header": {
    "title": "Bonjour Agent",
    "subtitle": "PDV Principal • TCH-POS-01"
  },
  "requiredStep": null,
  "operationalContext": {
    "ready": true,
    "trusted": true,
    "source": "SIGNED_DEVICE_BINDING",
    "outletId": "outlet_...",
    "outletName": "PDV Principal",
    "terminalId": "terminal_...",
    "terminalLabel": "TCH-POS-01",
    "salesSessionId": "session_..."
  },
  "session": {
    "open": true,
    "openedAt": "2026-05-21T12:00:00Z",
    "openedAtLabel": "08:00",
    "ticketCount": 47,
    "salesTotal": "2385.00 HTG"
  },
  "primaryDraw": {
    "drawId": "draw_...",
    "drawChannelId": "channel_...",
    "label": "Haïti • Texas • 10:00",
    "scheduledAt": "2026-05-21T15:00:00Z",
    "scheduledAtLabel": "2026-05-21 11:00",
    "cutoffAt": "2026-05-21T14:55:00Z",
    "cutoffLabel": "Clôture dans 24 min",
    "status": "OPEN"
  },
  "primaryAction": {
    "type": "SELL_TICKET",
    "label": "Vendre un ticket",
    "enabled": true,
    "route": "/sell"
  },
  "quickActions": [
    {"type": "RECENT_TICKETS", "label": "Tickets récents", "route": "/tickets", "enabled": true},
    {"type": "SESSION", "label": "Session", "route": "/session", "enabled": true},
    {"type": "PROFILE", "label": "Profil", "route": "/profile", "enabled": true}
  ],
  "widgets": [
    {
      "key": "session_status",
      "type": "POS_SESSION_STATUS",
      "data": {
        "open": true,
        "ticketCount": 47,
        "salesTotal": "2385.00 HTG"
      }
    },
    {
      "key": "primary_draw",
      "type": "POS_DRAW_STATUS",
      "data": {
        "label": "Haïti • Texas • 10:00",
        "cutoffLabel": "Clôture dans 24 min",
        "status": "OPEN"
      }
    }
  ],
  "navigation": [
    {"key": "sell", "label": "Vendre", "route": "/sell"},
    {"key": "tickets", "label": "Tickets", "route": "/tickets"},
    {"key": "session", "label": "Session", "route": "/session"},
    {"key": "profile", "label": "Profil", "route": "/profile"}
  ],
  "notices": []
}
```

### Operational context missing

```json
{
  "surface": "MOBILE_POS",
  "version": "home.v1",
  "header": {
    "title": "Configurer le poste",
    "subtitle": "Sélectionnez le point de vente et le terminal"
  },
  "requiredStep": {
    "type": "SELECT_OPERATIONAL_CONTEXT",
    "title": "Configurer le poste",
    "message": "Sélectionnez le point de vente et le terminal avant de vendre."
  },
  "operationalContext": {
    "ready": false,
    "trusted": false,
    "missing": ["OUTLET", "TERMINAL"]
  },
  "primaryAction": {
    "type": "SELECT_OPERATIONAL_CONTEXT",
    "label": "Configurer le poste",
    "enabled": true,
    "route": "/operational-context/select"
  },
  "quickActions": [],
  "widgets": [],
  "navigation": [
    {"key": "profile", "label": "Profil", "route": "/profile"}
  ],
  "notices": []
}
```

### Session closed

```json
{
  "surface": "MOBILE_POS",
  "version": "home.v1",
  "header": {
    "title": "Session fermée",
    "subtitle": "PDV Principal • TCH-POS-01"
  },
  "requiredStep": {
    "type": "OPEN_SESSION",
    "title": "Session fermée",
    "message": "Ouvrez une session pour commencer à vendre."
  },
  "operationalContext": {
    "ready": true,
    "trusted": true,
    "outletName": "PDV Principal",
    "terminalLabel": "TCH-POS-01"
  },
  "session": {
    "open": false
  },
  "primaryAction": {
    "type": "OPEN_SESSION",
    "label": "Ouvrir session",
    "enabled": true,
    "route": "/session/open"
  },
  "quickActions": [
    {"type": "PROFILE", "label": "Profil", "route": "/profile", "enabled": true}
  ],
  "widgets": [],
  "notices": []
}
```

## 5. Web cashier home endpoint

```http
GET /tenant/cashier/web-home
X-Tch-Surface: CASHIER_WEB
```

The web home can use widgets and may be backed by PageModel layout.

```json
{
  "surface": "CASHIER_WEB",
  "version": "home.v1",
  "header": {
    "title": "Caisse",
    "subtitle": "PDV Principal • TCH-POS-01",
    "displayName": "Agent"
  },
  "primaryAction": {
    "type": "SELL_TICKET",
    "label": "Vendre un ticket",
    "enabled": true,
    "route": "/cashier/sell"
  },
  "widgets": [
    {
      "key": "session_summary",
      "title": "Session",
      "type": "SUMMARY_CARD",
      "data": {
        "open": true,
        "openedAtLabel": "08:00",
        "ticketCount": 47,
        "salesTotal": "2385.00 HTG"
      }
    },
    {
      "key": "next_draw",
      "title": "Tirage actif",
      "type": "DRAW_CARD",
      "data": {
        "label": "Haïti • Texas • 10:00",
        "cutoffLabel": "Clôture dans 24 min",
        "status": "OPEN"
      }
    },
    {
      "key": "recent_tickets",
      "title": "Tickets récents",
      "type": "RECENT_TICKETS",
      "data": {
        "items": []
      }
    }
  ],
  "notices": []
}
```

## 6. Reposition old PageModel

Existing `private.dashboard.cashier` becomes web-only:

```text
private.dashboard.cashier.web
```

Recommended row set for web cashier:

```text
identity
overview
quick_sale
recent_tickets
next_draws
session
```

Move or hide from cashier POS:

```text
pending_approvals -> tenant admin / web admin
limits -> tenant admin or secondary web
top_selections -> web only, not mobile POS
```

## 7. Implementation placement

```text
features.cashier.home
  web/CashierHomeController
  app/CashierHomeService
  model/CashierHomeResponse
  model/HomeRequiredStep
  model/HomeAction
  model/HomeWidget
  model/ClientSurface
  mapper/

features.profile
  web/ProfileController
  app/ProfileService
  model/ProfileResponse
```

Features are the correct layer because they expose endpoints oriented around screens/flows and compose multiple core/catalog/platform sources without owning business invariants.

## 8. Data ownership

Cashier home BFF may ask:

```text
core.session     -> current/open session
core.terminal    -> terminal/current context
core.outlet      -> outlet display/capability
core.draw        -> next/open draw
core.sales       -> recent tickets/dashboard stats
platform.identity/profile -> user profile/capabilities
platform.tenantconfig -> feature flags/channels
```

No repositories in features.
No core internal imports.
Use CommandBus/QueryBus and public APIs only.
