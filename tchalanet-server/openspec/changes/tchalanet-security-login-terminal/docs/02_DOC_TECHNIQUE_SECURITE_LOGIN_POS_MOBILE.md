# DOC TECHNIQUE — Sécurité login Web/Mobile/POS et transactions

## 1. Architecture cible

La sécurité transactionnelle est répartie ainsi :

```text
Keycloak                 -> identité, tokens, sessions OIDC
Angular                  -> web admin, dashboards, orchestration UI
Flutter                  -> POS/mobile, auth locale, secure storage, binding
Spring common.context    -> TchRequestContext, filters, RLS bridge
platform.accesscontrol   -> permissions et evaluator
platform.communication   -> email/SMS/OTP
platform.audit           -> audit fonctionnel
platform.idempotence     -> idempotency records
core.terminal            -> terminal, assignment, device binding, activation
core.seller              -> seller business identity and outlet assignment
core.session             -> session de vente
core.sales               -> vente ticket et règles transactionnelles
core.payout              -> paiement de gains
core.offlinesync         -> validation technique offline
```

## 2. Pipeline HTTP Spring

Pipeline cible :

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
  -> @PreAuthorize / TchPermissionEvaluator
  -> CommandBus / QueryBus
  -> Handler
```

Il ne faut pas ajouter un `OperationalContextFilter` séparé. Le contexte opérationnel est attaché dans le pipeline existant.

## 3. Contexte global

`TchRequestContext` représente :

- tenant effectif ;
- actor/user ;
- scope API ;
- roles ;
- locale ;
- timezone ;
- request id ;
- idempotency key ;
- operational context optionnel.

Le tenant vient du JWT, de la policy de scope ou d’un override super admin audité. Le client ne fournit jamais un tenant source de vérité pour les endpoints `/tenant/**` et `/admin/**`.

## 4. OperationalRequestContext

Contrat :

```java
public record OperationalRequestContext(
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    OperationalContextSource source
) {}
```

Sources :

```java
public enum OperationalContextSource {
  SERVER_BOOTSTRAP,
  SIGNED_DEVICE_BINDING,
  ADMIN_SELECTION,
  CLIENT_CLAIM,
  NONE
}
```

Trusted :

```text
SERVER_BOOTSTRAP
SIGNED_DEVICE_BINDING
ADMIN_SELECTION
```

Untrusted :

```text
CLIENT_CLAIM
NONE
```

Les actions sensibles doivent appeler :

```java
ctx.trustedOperationalContextRequired();
```

## 5. Nouveaux concepts core.terminal

### 5.1 Terminal

```text
terminal
- id uuid pk
- tenant_id uuid not null
- outlet_id uuid null
- code varchar unique per tenant
- label varchar
- kind varchar: PHYSICAL | VIRTUAL
- surface varchar: POS | MOBILE | WEB | BACK_OFFICE
- status varchar: REGISTERED | PENDING_ACTIVATION | ACTIVE | LOCKED | REVOKED | RETIRED
- sync_state varchar: ONLINE | OFFLINE | SYNC_PENDING | SYNC_CONFLICT
- capabilities via terminal_capability table
- created_at timestamptz
- updated_at timestamptz
- version int
```

### 5.2 TerminalAssignment

```text
terminal_assignment
- id uuid pk
- tenant_id uuid not null
- terminal_id uuid not null
- user_id uuid not null
- status varchar: ACTIVE | REVOKED
- assigned_at timestamptz
- revoked_at timestamptz null
- created_at timestamptz
- updated_at timestamptz
```

Contraintes MVP :

```text
unique active physical terminal per user per tenant
unique active virtual phone terminal per user per tenant
```

### 5.3 TerminalDeviceBinding

```text
terminal_device_binding
- id uuid pk
- tenant_id uuid not null
- terminal_id uuid not null
- binding_type varchar: POS_DEVICE | MOBILE_APP | ADMIN_SELECTION
- binding_public_key text null
- binding_secret_hash text null
- device_fingerprint_hash text null
- verified_channel varchar null: SMS | EMAIL | ADMIN_MANUAL | QR
- status varchar: ACTIVE | REVOKED | EXPIRED
- bound_at timestamptz
- expires_at timestamptz null
- last_seen_at timestamptz null
- created_at timestamptz
- updated_at timestamptz
```

### 5.4 TerminalActivationChallenge

```text
terminal_activation_challenge
- id uuid pk
- tenant_id uuid not null
- terminal_id uuid not null
- user_id uuid not null
- challenge_type varchar: POS_PAIRING | MOBILE_OTP | ADMIN_PAIRING_CODE
- channel varchar: QR | SMS | EMAIL | ADMIN_MANUAL
- code_hash text not null
- expires_at timestamptz not null
- attempt_count int default 0
- max_attempts int default 5
- status varchar: PENDING | CONSUMED | EXPIRED | CANCELLED
- created_at timestamptz
- consumed_at timestamptz null
```

## 6. Commands et Queries

### Commands

```text
CreateTerminalCommand
AssignTerminalToUserCommand
LockTerminalCommand
RevokeTerminalCommand
CreateTerminalActivationChallengeCommand
VerifyTerminalActivationChallengeCommand
BindPhysicalTerminalDeviceCommand
ActivateVirtualPhoneTerminalCommand
SelectOperationalContextCommand
ClearOperationalContextCommand
```

### Queries

```text
ResolveOperationalContextQuery
ValidateTerminalForOperationQuery
GetCurrentOperationalContextQuery
ListTerminalsQuery
GetTerminalDetailQuery
CheckTenantPlanCapabilityQuery
```

## 7. Endpoints proposés

### Admin terminal

```http
POST   /admin/terminals
GET    /admin/terminals
GET    /admin/terminals/{terminalId}
POST   /admin/terminals/{terminalId}/assign
POST   /admin/terminals/{terminalId}/lock
POST   /admin/terminals/{terminalId}/revoke
```

### POS physique

```http
POST /tenant/terminals/{terminalId}/pairing-challenges
POST /tenant/terminals/{terminalId}/pair
GET  /tenant/me/operational-context
```

### Terminal virtuel téléphone

```http
POST /tenant/virtual-terminals/phone/activation-challenges
POST /tenant/virtual-terminals/phone/activate
GET  /tenant/me/operational-context
```

### Admin POS mode

```http
POST   /tenant/me/operational-context/select
GET    /tenant/me/operational-context
DELETE /tenant/me/operational-context
```

## 8. Headers client

### Angular web

```http
Authorization: Bearer <access_token>
Accept-Language: fr-HT
X-Request-Id: <uuid>
```

### Flutter POS

```http
Authorization: Bearer <access_token>
X-Request-Id: <uuid>
X-Device-Binding: <signed-binding-token>
X-Terminal-Id: <terminal-id>
X-Outlet-Id: <outlet-id>
X-Sales-Session-Id: <session-id>
Idempotency-Key: <uuid>
```

Seed local: le POS de demo peut utiliser `X-Device-Binding: local-dev-binding-token`.
Ce credential est reserve au developpement local et son hash est precharge dans
`V205__seed_outlet_terminal_pos.sql`.

### Flutter vente téléphone

```http
Authorization: Bearer <access_token>
X-Request-Id: <uuid>
X-Virtual-Terminal-Binding: <signed-binding-token>
X-Terminal-Id: <virtual-terminal-id>
X-Sales-Channel: PHONE
Idempotency-Key: <uuid>
```

## 9. Permission evaluator

Le web exprime les besoins avec :

```java
@PreAuthorize("hasPermission('ticket.sell')")
@PreAuthorize("hasPermission('ticket.sell.phone')")
@PreAuthorize("hasPermission('terminal.assign')")
```

`TchPermissionEvaluator` doit :

1. extraire tenant et user depuis `TchRequestContext` ou `Authentication` ;
2. normaliser la permission ;
3. appeler `CheckUserPermissionsHandler` ;
4. retourner `false` si deny ;
5. ne jamais appeler de repository directement.

## 10. Check plan / entitlement

Le check plan sert à contrôler les capacités commerciales du tenant :

```text
PHONE_SALES_ENABLED
MAX_PHYSICAL_TERMINALS_PER_USER
MAX_VIRTUAL_PHONE_TERMINALS_PER_USER
OFFLINE_SALES_ENABLED
MAX_OFFLINE_GRANT_PER_TERMINAL
```

Placement recommandé :

- `catalog.plan` pour les définitions de plan ;
- `core.subscription` pour l’état d’abonnement tenant ;
- `platform.entitlement` pour la résolution effective des capacités ;
- `core.terminal` consomme une query/API stable pour valider les limites.

## 11. Idempotence

Pour `POST /tenant/tickets`, `Idempotency-Key` est obligatoire.

Flow :

```text
TchContextFilter extrait Idempotency-Key
@RequireIdempotency(scope = SALES_SELL_TICKET) commence le record
SellTicketHandler crée la ressource
handler/aspect complète le record avec resource_id / response
replay retourne le même ticket
payload différent retourne 409
```

## 12. Audit

Audit obligatoire sur :

- terminal create/assign/activate/lock/revoke ;
- pairing challenge ;
- virtual phone activation ;
- admin pos mode selection ;
- ticket sell/cancel ;
- payout ;
- offline grant/sync ;
- super admin override ;
- permission changes.

## 13. RLS

Toutes les tables tenant-scoped doivent avoir :

```text
tenant_id not null
indexes tenant-aware
RLS policies
```

Les queries applicatives ne doivent pas ajouter un `WHERE tenant_id = ?` pour compenser RLS. La source de vérité est le contexte courant bindé.

## 14. Fail-fast validation order

Pour une vente :

```text
1. Auth token valide
2. TchRequestContext valide
3. permission ticket.sell ou ticket.sell.phone
4. Idempotency-Key présente
5. trustedOperationalContextRequired
6. terminal existe / tenant
7. terminal actif / non locked
8. terminal assigné au user
9. binding actif et compatible
10. outlet existe / actif
11. session existe / ouverte / compatible
12. seller résolu côté serveur et assigné à l'outlet
13. plan entitlement compatible
14. sales business gates: draw, cutoff, pricing, limits
15. transaction + audit + events after commit
```
