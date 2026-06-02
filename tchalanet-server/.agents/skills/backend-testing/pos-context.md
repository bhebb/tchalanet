# Backend Testing — POS Operational Context

Use this guidance when testing POS sell, payout, offline grant, and offline sync endpoints.

## Non-negotiable rule

POS operations require **trusted operational context** via verified terminal binding.

Endpoints that handle money, tickets, or payout MUST enforce:

- Terminal is tenant-owned and trusted (verified signature).
- Outlet is tenant-owned.
- Session is open and matches terminal + outlet + seller.
- Seller has the required permission/capability.

## Required test scenarios

For each POS endpoint (sell, payout, offline grant):

1. **Missing operational context** → 403 Forbidden or 400 Bad Request
2. **Terminal not tenant-owned** → 403 Forbidden
3. **Outlet not tenant-owned** → 403 Forbidden
4. **Session terminal/outlet/seller mismatch** → 403 Forbidden
5. **Closed session** → 409 Conflict or 403 Forbidden
6. **Permission removed** → 403 Forbidden
7. **Valid cashier + terminal + outlet + open session** → 200/201 success

## Test structure

```java
@Nested
class OperationalContextTests {
  
  @Test
  void missingOperationalContext_blocked() { }
  
  @Test
  void terminalNotTenantOwned_blocked() { }
  
  @Test
  void outletNotTenantOwned_blocked() { }
  
  @Test
  void sessionMismatch_blocked() { }
  
  @Test
  void closedSession_blocked() { }
  
  @Test
  void permissionRemoved_returns403() { }
  
  @Test
  void validContext_allowed() { }
}
```

## Fixtures

Use explicit fixtures for tenant/outlet/terminal/session/seller IDs.

Example:

```java
OperationalContext validContext = OperationalContext.of(
    TenantId.of(TENANT_UUID),
    OutletId.of(OUTLET_UUID),
    TerminalId.of(TERMINAL_UUID),
    SessionId.of(SESSION_UUID),
    UserId.of(SELLER_UUID)
);
```

Do not randomize IDs in operational context tests.
