# Design: Mobile startup, terminal binding, and i18n

## 1. Runtime principle

The mobile app must distinguish three independent context layers:

```text
TchRuntimeProfile     -> UI/device surface, screen class, capabilities
CurrentUserContext    -> authenticated user, tenant, roles, permissions
OperationalContext    -> terminal, outlet, sales session, source/trust
```

These contexts must not be merged into a single object because they change at different times.

## 2. Post-login routing

```text
Keycloak login OK
  -> GET /tenant/profile/me/current
     -> DEVICE_NOT_ENROLLED     => show device blocked/enrollment help screen
     -> SESSION_CLOSED          => show open session screen
     -> READY_FOR_CASHIER       => GET /tenant/cashier/home
     -> ADMIN_MODE_AVAILABLE    => admin dashboard
     -> PLAN_FEATURE_BLOCKED    => plan/entitlement blocked screen
```

`profile/current` guides the application. It does not grant final rights for critical operations.

## 3. Backend security stance

Any client can bypass Flutter and call the backend directly. Therefore:

- UI state is never security.
- Headers are never trusted by themselves.
- `profile/current` is never a final authorization proof.
- Critical handlers revalidate plan, permission, device binding, terminal, outlet, session, cutoff, limits, and idempotency.

## 4. Endpoint behavior matrix

| Endpoint | Device not enrolled | Session closed | Purpose |
| --- | --- | --- | --- |
| `GET /tenant/profile/me/current` | `200` + `startupState=DEVICE_NOT_ENROLLED` | `200` + `startupState=SESSION_CLOSED` | Bootstrap/diagnostic |
| `GET /tenant/cashier/home` | `403 device.not_enrolled` | `409 session.required` | Screen content |
| `POST /tenant/seller/tickets/preview` | `403 device.not_enrolled` | `409 session.required` | Pre-validation/pricing |
| `POST /tenant/seller/tickets/sell` | `403 device.not_enrolled` | `409 session.required` | Final sale |
| `POST /tenant/sessions/open` | `403 device.not_enrolled` | allowed if terminal/outlet valid | Session transition |
| `POST /admin/devices/enroll` | depends on admin permission | not relevant | Admin device management |

## 5. Seller preview/sell

Preview validates a draft and returns price, warnings, blockers, labels, and available next actions.

Sell creates the final ticket and must:

- require `Idempotency-Key`;
- revalidate even if preview was done;
- reject if plan/subscription changed;
- reject if device/terminal/outlet/session changed;
- reject if draw cutoff passed;
- reject if limits now block the sale;
- snapshot critical receipt labels.

## 6. I18n model

Tchalanet uses two i18n layers:

```text
Flutter ARB/localizations
  -> stable UI labels owned by the app

Backend catalog.i18n
  -> system texts, backend actions, blockers, PageModel, documents, ticket PDF/print
```

Backend action/blocker contracts should expose:

```json
{
  "code": "SESSION_CLOSED",
  "messageKey": "seller.blocker.session_closed",
  "fallbackMessage": "Vous devez ouvrir une session avant de vendre.",
  "params": {}
}
```

The app may resolve the key from a backend dictionary. If absent, it displays `fallbackMessage`.

## 7. Scope-aware i18n resolution

The current `catalog.i18n` model uses `I18nOverrideLevel`:

```text
GLOBAL
TENANT
```

Resolution rules:

```text
resolveLocale(locale, ctx):
  GLOBAL + TENANT only when ctx has tenant and scope is not PLATFORM

resolveLocaleForTenant(locale, tenantId):
  GLOBAL + TENANT for the explicit tenant
```

PLATFORM scope must never implicitly merge all visible tenant overrides.

## 8. Ticket/PDF/print i18n

Ticket/PDF/print renderers must not translate. They receive a resolved document model.

At sell time, store:

- resolved language;
- critical label snapshot;
- tenant/outlet display data needed for reprint;
- document template/version if applicable.

This prevents old tickets from changing text after tenant i18n overrides are edited.
