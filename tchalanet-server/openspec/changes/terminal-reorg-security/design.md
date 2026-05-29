# Design — Terminal Reorg + Security Reinforcement

## 1. Terminal mental model

A terminal is the operational identity of a selling device/app instance.

A terminal binding is the durable link between a declared terminal and a real POS/mobile installation.

```text
Tenant
  └── Outlet
        └── Terminal
              └── TerminalBinding
                    ├── bindingPublicKey
                    ├── publicKeyAlgorithm
                    ├── publicKeyHash
                    ├── credentialHash
                    ├── deviceFingerprintHash
                    └── state
```

For sensitive actions, the backend must verify:

```text
user authenticated
+ tenant context valid
+ trusted operational context
+ terminal active/unlocked
+ binding active
+ device signature valid
+ nonce not replayed
+ outlet/session/seller match
+ action-specific business rules
```

## 2. Controller reorganization

Canonical admin controllers:

```text
core.terminal.internal.infra.web.admin.TerminalAdminLifecycleController
  POST   /admin/terminals
  DELETE /admin/terminals/{terminalId}
  PATCH  /admin/terminals/{terminalId}/lock
  PATCH  /admin/terminals/{terminalId}/unlock

core.terminal.internal.infra.web.admin.TerminalAdminAssignmentController
  POST /admin/terminals/{terminalId}/assign-outlet
  POST /admin/terminals/{terminalId}/assign-user
  POST /admin/terminals/{terminalId}/activate-for-user

core.terminal.internal.infra.web.admin.TerminalAdminMetadataController
  PATCH /admin/terminals/{terminalId}/metadata

core.terminal.internal.infra.web.admin.TerminalAdminOperationalControlsController
  PATCH /admin/terminals/{terminalId}/operational-controls/{control}

core.terminal.internal.infra.web.admin.TerminalAdminQueryController
  GET /admin/terminals
  GET /admin/terminals/{terminalId}
  GET /admin/terminals/offline
  GET /admin/terminals/sync-pending
```

Tenant runtime controllers:

```text
core.terminal.internal.infra.web.tenant.TerminalActivationController
  POST /tenant/terminals/{terminalId}/activation-challenges
  POST /tenant/terminals/{terminalId}/activation-challenges/{challengeId}/verify

core.terminal.internal.infra.web.tenant.VirtualPhoneTerminalActivationController
  POST /tenant/virtual-terminals/phone/{terminalId}/activation-challenges
  POST /tenant/virtual-terminals/phone/{terminalId}/activation-challenges/{challengeId}/verify

core.terminal.internal.infra.web.tenant.TerminalTenantRuntimeController
  GET  /tenant/terminals/current
  GET  /tenant/terminals/{terminalId}/status
  POST /tenant/terminals/{terminalId}/heartbeat
  POST /tenant/terminals/{terminalId}/sync-state
```

## 3. Challenge and binding

Create challenge:

```text
request -> CreateTerminalActivationChallengeCommand
handler validates terminal/user/feature
handler generates secure 6-digit OTP
handler stores codeHash only
handler delivers code according to deliveryMode
```

Verify challenge:

```text
request -> VerifyTerminalActivationChallengeCommand
handler validates challenge, terminal, tenant, type, expiry, attempts, codeHash
handler computes credentialHash
handler computes publicKeyHash if publicKey provided
handler creates TerminalBinding
handler marks challenge consumed
```

Recommended command shape:

```java
public record VerifyTerminalActivationChallengeCommand(
    TenantId tenantId,
    TerminalId terminalId,
    TerminalActivationChallengeId challengeId,
    UserId verifiedBy,
    String clearCode,
    TerminalBindingType bindingType,
    String bindingPublicKey,
    TerminalPublicKeyAlgorithm publicKeyAlgorithm,
    String bindingCredential,
    String deviceFingerprintHash,
    UserId actorId
) {}
```

The controller should not hash the credential. Hashing belongs in the application service/handler.

## 4. Device proof for POS -> backend

The POS/mobile signs a canonical payload:

```text
purpose
method
path
bodyHash
terminalId
bindingId
outletId
sessionId
nonce
signedAt
```

Backend validates:

```text
binding exists and active
terminal active/unlocked
publicKey algorithm supported
signature valid
signedAt within clock skew window
nonce unused for binding
purpose matches endpoint
```

Suggested API:

```java
public record VerifyTerminalDeviceProofQuery(
    TenantId tenantId,
    TerminalId terminalId,
    TerminalBindingId bindingId,
    TerminalProofPurpose purpose,
    String method,
    String path,
    String bodyHash,
    Instant signedAt,
    String nonce,
    String signature
) {}
```

Result should not expose key material:

```java
public sealed interface VerifyTerminalDeviceProofResult {
    record Trusted(
        TerminalId terminalId,
        TerminalBindingId bindingId,
        OutletId outletId,
        UserId assignedUserId,
        TerminalBindingType bindingType
    ) implements VerifyTerminalDeviceProofResult {}

    record Rejected(String code) implements VerifyTerminalDeviceProofResult {}
}
```

## 5. Backend signed grant

For offline grants, direction is reversed:

```text
POS signs grant request with POS private key.
Backend verifies with POS public key from terminal_binding.
Backend creates OfflineGrant.
Backend signs OfflineGrant with backend private key.
POS verifies grant with backend public key.
```

The backend does not sign with the POS public key. The POS public key is used by the backend to verify POS signatures. The backend signs with its own private key.

Grant response shape:

```json
{
  "grant": {
    "type": "OFFLINE_GRANT",
    "version": 1,
    "grantId": "...",
    "tenantId": "...",
    "terminalId": "...",
    "bindingId": "...",
    "outletId": "...",
    "sellerId": "...",
    "sessionId": "...",
    "businessDate": "2026-05-29",
    "validFrom": "...",
    "validUntil": "...",
    "allowedPurposes": ["OFFLINE_SELL"],
    "limits": {
      "maxTickets": 50,
      "maxTotalAmount": "5000.00",
      "maxStakePerTicket": "500.00",
      "currency": "HTG"
    },
    "issuedAt": "...",
    "issuer": "tchalanet-backend",
    "keyId": "server-signing-key-2026-01"
  },
  "signature": "base64url...",
  "signatureAlgorithm": "Ed25519",
  "keyId": "server-signing-key-2026-01"
}
```

## 6. Suggested table strategy

V1 simple:

```text
terminal_binding
  id
  tenant_id
  terminal_id
  binding_type
  state
  binding_public_key nullable
  public_key_algorithm nullable
  public_key_hash nullable
  credential_hash not null
  device_fingerprint_hash not null
  bound_by
  bound_at
  last_seen_at
  revoked_at
```

V2 rotation-ready:

```text
terminal_binding_key
  id
  tenant_id
  binding_id
  algorithm
  public_key
  public_key_hash
  active_from
  active_until
  rotated_at
```

V1 can keep key columns directly on `terminal_binding`. Add child table only when rotation/history is needed.
