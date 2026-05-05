# Security Flows Guide

> **Status**: NORMATIVE POINTER  
> **Scope**: OpenSpec context for security-related changes  
> **Source of truth**: `tchalanet-docs/docs/01-architecture/flows/`

---

## Purpose

This context pack points agents and contributors to the canonical security flow documents.

OpenSpec should not duplicate the full security flow logic. It should reference the canonical flow documents and use them as implementation guidance.

---

## Authentication Flow

Canonical document:

```text
tchalanet-docs/docs/01-architecture/flows/authentication-flow.md
```

Covers:

- Keycloak JWT authentication;
- Spring Security resource-server setup;
- `ApiScope`;
- `UserBootstrapFilter`;
- `TchContextFilter`;
- `TchRequestContext`;
- super-admin overrides;
- RLS context integration;
- web/mobile header contract.

---

## Permission Flow

Canonical document:

```text
tchalanet-docs/docs/01-architecture/flows/permission-flow.md
```

Covers:

- `core.accesscontrol` responsibility;
- Spring Method Security;
- `TchPermissionEvaluator`;
- `CheckUserPermissionsQuery`;
- V1 role-derived permissions;
- V2 DB-driven permissions;
- controller security rules.

---

## Audit Flow

Future canonical document:

```text
tchalanet-docs/docs/01-architecture/flows/audit-flow.md
```

Will cover:

- audit annotation;
- `LogAuditEventCommand`;
- after-commit behavior;
- Envers and audit tables;
- super-admin override traceability.

---

## Rules for OpenSpec changes

- Do not duplicate full flow logic in OpenSpec.
- Reference these flow docs from proposals/designs when touching authentication, authorization, context, RLS or audit.
- If implementation changes a flow, update the canonical flow doc first or in the same change.
- Controllers must remain thin.
- Authentication/context belongs to `common.security` / `common.context`.
- Authorization decisions belong to `core.accesscontrol`.
- Tenant isolation belongs to PostgreSQL RLS, with context set by the server.
