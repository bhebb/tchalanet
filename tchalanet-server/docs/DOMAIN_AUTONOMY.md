# Autonomy Domain Documentation

## Overview

The Autonomy Domain manages the approval and authorization requirements for financial transactions within the Tchalanet betting system. It implements a hierarchical policy system that determines whether transactions require approval based on user roles, organizational structure, and transaction characteristics.

## Key Concepts

### Autonomy Policy Rule
An `AutonomyPolicyRule` is the core domain entity that defines approval requirements for a specific target entity (TENANT, OUTLET, TERMINAL, or AGENT).

**Fields:**
- `id`: Unique identifier
- `tenantId`: Tenant organization identifier
- `targetType`: Type of target (TENANT/OUTLET/TERMINAL/AGENT)
- `targetId`: Specific target entity identifier
- `level`: Autonomy level (NONE/PARTIAL/FULL)
- `requireApprovalOnBlock`: Whether blocked transactions need approval
- `approvalRole`: Required role for approval (OPERATOR/ADMIN)
- `enabled`: Whether the rule is active
- `startsAt/endsAt`: Time validity window
- `version`: Optimistic locking version

### Autonomy Resolver
The `AutonomyResolver` service determines the applicable autonomy policy for a given transaction context by following a hierarchical resolution:

1. **Agent-specific policy** (most specific)
2. **Terminal-specific policy**
3. **Outlet-specific policy**
4. **Tenant-wide policy**
5. **Default policy** (PARTIAL autonomy, OPERATOR approval)

### Resolved Autonomy
`ResolvedAutonomy` represents the effective autonomy settings after policy resolution, containing:
- `level`: Effective autonomy level
- `requireApprovalOnBlock`: Whether approval is needed for blocked transactions
- `approvalRole`: Required approval role

## Autonomy Levels

### NONE
- No autonomy granted
- All transactions require approval
- Most restrictive setting

### PARTIAL
- Partial autonomy based on other rules
- Some transactions may proceed without approval
- Default level for new tenants

### FULL
- Complete autonomy
- Transactions proceed without approval requirements
- Least restrictive setting

## Approval Roles

### OPERATOR
- Standard operator role
- Basic approval permissions
- Default approval role

### ADMIN
- Administrative role
- Elevated approval permissions
- Can approve higher-risk transactions

## Target Types

### TENANT
- Applies to entire tenant organization
- Broadest scope
- Affects all users and terminals

### OUTLET
- Applies to specific physical location
- Medium scope
- Affects all terminals at that outlet

### TERMINAL
- Applies to specific terminal device
- Narrow scope
- Affects transactions from that terminal

### AGENT
- Applies to specific user/agent
- Most specific scope
- Personal autonomy settings

## Integration with Limit Policy

The autonomy system works in conjunction with the limit policy system:

1. **Limit Evaluation**: Transactions are first evaluated against financial limits
2. **Autonomy Resolution**: If limits are breached, autonomy policies determine approval requirements
3. **Decision Matrix**:
   - `ALLOW`: Transaction proceeds
   - `WARN`: Transaction proceeds with notification
   - `BLOCK + No Approval Required`: Transaction rejected
   - `BLOCK + Approval Required`: Transaction pending approval

## Usage Examples

### Creating an Autonomy Policy
```java
var policy = new AutonomyPolicyRule(
    null, // auto-generated ID
    tenantId,
    AutonomyTargetType.TERMINAL,
    terminalId,
    AutonomyLevel.PARTIAL,
    true, // require approval on block
    ApprovalRole.OPERATOR,
    true, // enabled
    null, // no start time
    null, // no end time
    0L   // initial version
);
```

### Resolving Autonomy for a Transaction
```java
ResolvedAutonomy autonomy = autonomyResolver.resolve(
    tenantId,
    agentId,
    terminalId,
    outletId,
    Instant.now()
);

// Check if approval is needed
if (limitOutcome == BLOCK && autonomy.requireApprovalOnBlock()) {
    // Require approval from autonomy.approvalRole()
}
```

## Database Schema

### autonomy_policy_rule Table
```sql
CREATE TABLE autonomy_policy_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,
  tenant_id uuid NOT NULL,
  target_type varchar(20) NOT NULL,
  target_id uuid NOT NULL,
  level varchar(10) NOT NULL,
  require_approval_on_block boolean NOT NULL DEFAULT true,
  approval_role varchar(10),
  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);
```

### Indexes
- Unique index on `(tenant_id, target_type, target_id)` for active policies
- Lookup index on `(tenant_id, target_type, target_id, enabled)`

## API Endpoints

### Tenant Autonomy Management
- `GET /api/tenant/autonomy` - Get tenant autonomy policy
- `PUT /api/tenant/autonomy` - Update tenant autonomy policy

### Agent Autonomy Management
- `GET /api/tenant/agents/{agentId}/autonomy` - Get agent autonomy policy
- `PUT /api/tenant/agents/{agentId}/autonomy` - Update agent autonomy policy

## Business Rules

1. **Hierarchy Priority**: More specific targets override general ones
2. **Time Validity**: Policies only apply within their active time windows
3. **Default Fallback**: System provides sensible defaults when no policy exists
4. **Soft Deletes**: Policies are soft-deleted to maintain audit trails
5. **Versioning**: Optimistic locking prevents concurrent modification conflicts

## Error Handling

### Problem Details
Autonomy-related errors use structured `ProblemDetail` responses:

```json
{
  "type": "about:blank",
  "title": "Limit blocked",
  "status": 409,
  "detail": "Limit breach blocked",
  "operationType": "SALE",
  "limitOutcome": "BLOCK",
  "limitReasons": [...],
  "approvalRequired": true,
  "requiredRole": "OPERATOR"
}
```

## Testing Considerations

- Test hierarchy resolution with multiple overlapping policies
- Verify time-based activation/deactivation
- Test integration with limit policy outcomes
- Validate approval workflow integration
- Check proper error responses for blocked transactions

## Future Enhancements

- **Dynamic Policies**: Time-based or event-driven policy changes
- **Advanced Targeting**: Geographic or role-based targeting
- **Approval Workflows**: Multi-level approval chains
- **Audit Logging**: Comprehensive approval decision tracking
- **Policy Templates**: Predefined policy configurations
