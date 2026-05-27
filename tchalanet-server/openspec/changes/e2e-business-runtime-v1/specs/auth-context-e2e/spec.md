# Spec: Auth and Context E2E

## ADDED Requirements

### Requirement: Roles can authenticate

Super admin, tenant admin and cashier roles SHALL authenticate through E2E helpers.

### Requirement: Tenant context is authoritative

Tenant-scoped endpoints SHALL use current context and not trust client-supplied tenant IDs.
