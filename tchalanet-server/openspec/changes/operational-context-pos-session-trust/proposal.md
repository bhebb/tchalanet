# Change: operational-context-pos-session-trust

> Status: PROPOSED
> Supersedes/corrects: previous operational-context-offline-sync trust/context sections
> Scope: `common.context`, `TchContextFilter`, `core.session`, `core.offlinesync`, audit metadata, web API conventions

## Summary

Regenerate the operational POS context design with the corrected rule:

```text
HTTP request
  headers -> OperationalContextHint
  body    -> business payload

TchContextFilter
  parses headers only
  attaches a POS hint to TchRequestContext
  parses tenant override separately
  does not validate terminal/outlet/session

Handler
  requires hint when action needs POS context
  asks core.session to resolve/validate the POS frame late
  applies action-specific policy
```

The POS operational context transported by headers is a **claim**, never a business proof. The authoritative POS frame is produced only by `core.session` via `ResolvePosOperationContextQuery`.

## Core decisions

```text
D1. POS context IDs are transmitted by headers, never by request body.
D2. TchContextFilter parses POS headers and attaches an OperationalContextHint only.
D3. The hint carries source + server-derived trust, but it is not authoritative.
D4. core.session owns authoritative POS frame resolution.
D5. Handlers use QueryBus.ask(...) to call ResolvePosOperationContextQuery.
D6. ADMIN_SELECTION is not STRONG in V1 unless a server-issued signed selection token exists.
D7. No persisted admin POS selection in V1.
D8. Admin POS V1 may accept CLIENT_CLAIM/WEAK if core.session fully validates the frame and policy allows it.
D9. Offline grant and offline sync must not rely on CLIENT_CLAIM/WEAK alone.
D10. Super-admin tenant override is separate from POS operational context.
D11. Audit logs the effective validated frame, not raw claimed headers.
```

## Why

The previous design was close, but needed four corrections:

1. **Headers are not proof**: terminal/outlet/session IDs from headers must never be treated as validated POS context.
2. **Source and trust must be separated**: trust is a server conclusion derived from source + proof, not something the client can declare.
3. **Session owns POS coherence**: terminal lifecycle is not enough; only session can validate terminal + outlet + seller + session status + action coherence.
4. **Tenant override is not POS context**: `X-Tch-Tenant-Override` changes the effective tenant context after permission/reason validation; it must not become an `OperationalContextSource`.

## Canonical headers

```http
X-Tch-Terminal-Id
X-Tch-Outlet-Id
X-Tch-Sales-Session-Id
X-Tch-Operational-Source
X-Tch-Tenant-Override
X-Tch-Override-Reason
```

`X-Tch-Operational-Trust` is forbidden as an input. If present, it is ignored or rejected according to implementation choice, but it must never influence server trust.

## Trust model

```java
public enum OperationalContextSource {
    NONE,
    CLIENT_CLAIM,
    SIGNED_DEVICE_BINDING,
    ADMIN_SELECTION
}

public enum OperationalContextTrust {
    NONE,
    WEAK,
    STRONG
}
```

`SUPER_ADMIN_OVERRIDE` is deliberately not in `OperationalContextSource`. It belongs to tenant/effective context resolution.

## V1 action decisions

| Action | WEAK accepted? | STRONG required? | Decision |
|---|---:|---:|---|
| Admin POS online | Yes | No in V1 | Accept `CLIENT_CLAIM/WEAK` if `core.session` validates all IDs/status/permissions. |
| Seller POS online sale | Temporary | Target | Feature-flag path to require `SIGNED_DEVICE_BINDING/STRONG`. |
| Offline grant | No | Yes | Require signed device binding or equivalent server-verifiable device proof. |
| Offline sync | No | Yes | Require grant + device proof + signature. |
| POS payout | Avoid WEAK | Recommended | Money-sensitive; prefer strong proof. |
| Super-admin tenant override | N/A | Yes | Separate tenant override flow with permission + reason + audit. |

## Out of scope

- Final cryptographic format for signed device binding.
- Signed admin-selection token implementation.
- Persisted admin POS selection.
- V2 validation cache.
- Full offline submission review UI.

## Acceptance

Implementation is accepted when:

```text
OperationalContextSource and OperationalContextTrust are separate.
OperationalContextTrust is server-derived only.
POS IDs are parsed from headers only, never from body.
TchContextFilter attaches OperationalContextHint and performs no POS DB validation.
ResolvePosOperationContextQuery lives in core.session.api.query.
ValidatedPosOperationContext is the only validated POS frame used by sensitive handlers.
Handlers call queryBus.ask(...), not queryBus.execute(...), for POS resolution.
ADMIN_SELECTION remains WEAK in V1 unless a signed server selection token is verified.
SUPER_ADMIN_OVERRIDE is not an OperationalContextSource.
Offline grant rejects WEAK-only POS claims.
Audit records the validated operational frame used by sensitive actions.
```
