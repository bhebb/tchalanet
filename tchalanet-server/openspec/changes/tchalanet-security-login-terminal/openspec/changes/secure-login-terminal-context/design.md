# Design — secure-login-terminal-context

## 1. Request pipeline

```text
BearerTokenAuthenticationFilter
  -> UserBootstrapFilter
  -> TchContextFilter
      -> ApiScopeResolver
      -> TchRequestContextFactory
      -> TenantContextResolver
      -> ActorContextResolver
      -> OperationalContextResolver
      -> TchContextBinder.bind(finalCtx)
  -> Controller
  -> Method Security / TchPermissionEvaluator
  -> CommandBus / QueryBus
```

## 2. Operational context

`OperationalContextResolver` reads headers and server-side binding data. Raw client claims are considered `CLIENT_CLAIM` unless validated against a binding or admin selection.

Trusted sources:

- `SERVER_BOOTSTRAP`
- `SIGNED_DEVICE_BINDING`
- `ADMIN_SELECTION`

Untrusted sources:

- `CLIENT_CLAIM`
- `NONE`

Sensitive handlers must call `ctx.trustedOperationalContextRequired()`.

## 3. Terminal security model

### Terminal types

- `PHYSICAL_POS`
- `VIRTUAL_PHONE`
- `VIRTUAL_WEB`

### Terminal statuses

- `PENDING_ACTIVATION`
- `ACTIVE`
- `LOCKED`
- `REVOKED`
- `EXPIRED`

### Terminal capabilities

- `SELL_TICKET`
- `PHONE_SALE`
- `OFFLINE_SELL`
- `PAYOUT`
- `ADMIN_POS_MODE`

## 4. Plan / entitlement checks

Terminal creation/activation and phone sale use cases must check tenant capabilities:

- `PHONE_SALES_ENABLED`
- `MAX_PHYSICAL_TERMINALS_PER_USER`
- `MAX_VIRTUAL_PHONE_TERMINALS_PER_USER`
- `OFFLINE_SALES_ENABLED`

## 5. Authentication flows

### Angular Web

Uses Keycloak Authorization Code + PKCE. No operational context by default.

### Flutter POS/Mobile

Uses Keycloak Authorization Code + PKCE or approved mobile flow. Local auth only unlocks secure storage. Refresh token is used to obtain a new access token. Backend still validates operational context.

### OTP

OTP is for activation, reset, change device, high-risk actions. OTP is not required on every login.

## 6. Idempotency

Sales endpoint requires `Idempotency-Key` with scope `SALES_SELL_TICKET`.

## 7. Audit

Audit all terminal, binding, phone sales activation, operational context selection and sensitive transaction events.

