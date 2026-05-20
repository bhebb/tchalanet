# Spec — Operational Context During Platform Migration

## Requirement: HTTP context remains canonical

HTTP request context SHALL continue to be produced by the HTTP filter chain.

### Scenario: platform accesscontrol checks permissions

Given an authenticated tenant request  
When `platform.accesscontrol` checks permissions  
Then it uses the already-bound request context or explicit context data  
And it does not parse JWT or resolve tenant from the request body.

## Requirement: RLS remains datasource-owned

RLS session variables SHALL continue to be applied by the datasource bridge.

### Scenario: tenantconfig moves to platform

Given tenantconfig persistence moves to `platform.tenantconfig.internal.persistence`  
When a tenant-scoped read executes  
Then tenant isolation is enforced by PostgreSQL RLS  
And no new Java tenant filter is added to compensate.

## Requirement: events carry context metadata

Events crossing modules SHALL carry tenant/actor/correlation metadata when required.

### Scenario: audit listener receives event

Given a core command publishes a public event  
When `platform.audit` records it asynchronously or after commit  
Then the audit record includes tenant id, actor id and request/correlation id when available.

## Requirement: batch and scheduler context is explicit

Batch and scheduler flows SHALL bind context explicitly before DB access.

### Scenario: scheduled communication retry

Given `platform.communication` retries failed deliveries from a scheduler  
When it reads tenant-scoped delivery state  
Then it binds a platform or tenant context intentionally  
And restores/clears context afterward.
