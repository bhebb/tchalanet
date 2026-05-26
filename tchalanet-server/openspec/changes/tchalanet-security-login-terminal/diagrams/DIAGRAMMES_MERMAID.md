# Diagrammes Mermaid — Login Web/Mobile/POS et sécurité transactionnelle

## 1. Login Web Angular / Admin

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur Web
  participant A as Angular Web
  participant K as Keycloak
  participant S as Spring API
  participant F as TchContextFilter
  participant P as TchPermissionEvaluator
  participant B as CommandBus/QueryBus

  U->>A: Ouvre /admin ou /tenant
  A->>K: Redirect login OIDC + PKCE
  U->>K: Username/email + password (+ MFA)
  K-->>A: access_token + refresh_token
  A->>S: API call Authorization: Bearer token
  S->>F: Construire TchRequestContext
  F-->>S: tenant, actor, scope, roles
  S->>P: hasPermission(...)
  P->>B: CheckUserPermissionsQuery/Handler
  B-->>P: allow / deny
  P-->>S: true / false
  S-->>A: dashboard / 403
```

## 2. Login Flutter POS avec device binding

```mermaid
sequenceDiagram
  autonumber
  actor V as Vendeur
  participant M as Flutter POS
  participant L as Local Auth
  participant K as Keycloak
  participant T as core.terminal API
  participant S as Spring API
  participant O as OperationalContextResolver
  participant SE as core.session

  V->>M: Ouvre app POS
  M->>L: Face ID / empreinte / PIN local
  L-->>M: OK
  M->>K: Refresh token -> access token
  K-->>M: access_token
  M->>S: Bearer + X-Device-Binding + TerminalId + SessionId
  S->>O: ResolveOperationalContext
  O->>T: Validate terminal + binding + assignment
  T-->>O: terminal trusted
  O->>SE: Validate/open sales session
  SE-->>O: session OK
  O-->>S: OperationalRequestContext(source=SIGNED_DEVICE_BINDING)
  S-->>M: POS home ready
```

## 3. Activation POS physique

```mermaid
sequenceDiagram
  autonumber
  actor Admin as Tenant Admin
  actor V as Vendeur
  participant W as Angular Admin
  participant M as Flutter POS
  participant K as Keycloak
  participant T as core.terminal
  participant C as platform.communication

  Admin->>W: Crée terminal PHYSICAL + POS
  W->>T: CreateTerminalCommand
  Admin->>W: Assigne user + outlet
  W->>T: AssignTerminalToUserCommand
  V->>M: Scanne QR / saisit code
  M->>K: Login vendeur
  K-->>M: access_token
  M->>T: Verify pairing challenge
  T->>T: validate tenant/user/terminal/outlet
  T-->>M: SIGNED_DEVICE_BINDING
  T-->>W: terminal ACTIVE
```

## 4. Activation terminal virtuel téléphone

```mermaid
sequenceDiagram
  autonumber
  actor Admin as Tenant Admin
  actor V as Vendeur
  participant W as Angular Admin
  participant M as Flutter Mobile
  participant E as platform.entitlement
  participant T as core.terminal
  participant C as platform.communication
  participant K as Keycloak

  Admin->>W: Active vente téléphone pour vendeur
  W->>E: Check PHONE_SALES_ENABLED
  E-->>W: allowed
  W->>T: Create/Assign VIRTUAL + MOBILE terminal
  V->>M: Demande activation
  M->>T: Create activation challenge
  T->>C: Send OTP/email/admin code
  C-->>V: Code
  V->>M: Saisit code
  M->>K: Login/refresh token
  K-->>M: access_token
  M->>T: Verify challenge + activate
  T-->>M: virtual binding signed
```

## 5. Vente ticket sécurisée

```mermaid
sequenceDiagram
  autonumber
  actor V as Vendeur
  participant M as Flutter POS/Mobile
  participant S as Spring API
  participant I as IdempotencyAspect
  participant F as TchContextFilter
  participant P as PermissionEvaluator
  participant O as OperationalContextResolver
  participant T as core.terminal
  participant SS as core.session
  participant SA as core.sales
  participant A as platform.audit

  V->>M: Confirme vente
  M->>S: POST /tenant/tickets + Idempotency-Key + binding
  S->>F: Build TchRequestContext
  S->>I: begin(scope=SALES_SELL_TICKET)
  S->>P: hasPermission(ticket.sell or ticket.sell.phone)
  P-->>S: allow
  S->>O: trustedOperationalContextRequired
  O->>T: validate terminal/binding/assignment
  T-->>O: OK
  O->>SS: validate session/outlet/user match
  SS-->>O: OK
  S->>SA: SellTicketCommand
  SA->>SA: cutoff/pricing/limits/idempotent create
  SA-->>S: TicketResult
  S->>I: complete(resourceId=ticketId)
  S->>A: audit SELL after commit
  S-->>M: ticket sold
```

## 6. Activité — ouverture app mobile/POS

```mermaid
flowchart TD
  A([Début]) --> B[Ouvrir app Flutter]
  B --> C{Auth locale activée?}
  C -- Oui --> D[Face ID / empreinte / PIN]
  C -- Non --> H[Login Keycloak]
  D --> E{OK?}
  E -- Non --> X[Refuser accès local]
  E -- Oui --> F{Refresh token présent?}
  F -- Non --> H
  F -- Oui --> G[Refresh token -> access token Keycloak]
  G --> I{Refresh accepté?}
  I -- Non --> H
  I -- Oui --> J[Appeler Spring avec token + binding]
  H --> K[Username/téléphone + password]
  K --> L{Login OK?}
  L -- Non --> H
  L -- Oui --> J
  J --> M[Spring valide TchRequestContext]
  M --> N[Spring résout OperationalRequestContext]
  N --> O{Context trusted?}
  O -- Non --> P[Accès lecture possible, vente bloquée]
  O -- Oui --> Q[Accueil POS / phone sales]
  Q --> R([Fin])
```

## 7. Composants backend

```mermaid
flowchart LR
  A[Angular Web] --> K[Keycloak]
  F[Flutter POS/Mobile] --> K
  A --> API[Spring API]
  F --> API
  K --> API

  API --> Ctx[TchContextFilter / TchRequestContext]
  Ctx --> Authz[TchPermissionEvaluator]
  Authz --> AC[platform.accesscontrol]
  Ctx --> OCR[OperationalContextResolver]
  OCR --> Terminal[core.terminal]
  OCR --> Outlet[core.outlet]
  OCR --> Session[core.session]
  API --> Idem[platform.idempotence]
  API --> Audit[platform.audit]
  API --> Sales[core.sales]
  Terminal --> Ent[platform.entitlement / catalog.plan]
  Terminal --> Comm[platform.communication]
```
