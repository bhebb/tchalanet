# Platform Ops Jobs + Support Admin V0

## Why

Two platform-admin surfaces are needed before the weekly cutoff:

- Operations Jobs must expose the existing Spring Batch registry, manual start, and execution history in a usable way.
- Tenant support mode must let a super admin select a tenant and enter tenant-admin support context without duplicating tenant admin pages.

## What

- Finish the web Jobs page around existing backend batch APIs.
- Keep gate/cache management out of this slice.
- Replace the support-tenant placeholder with a tenant selection surface that starts support admin access.
- Add backend support-session endpoints only if the current frontend contract has no server implementation.
- Align platform Ops lifecycle actions with Spring Batch as the single execution engine:
  - guided draw/result Ops endpoints launch registered Spring Batch jobs;
  - direct commands are reserved for targeted human actions such as manual/override/confirm/cache clear.

## Impact

- Web: platform operations jobs page and support tenant route.
- Server: possible platform tenant admin-access endpoint.
- Server/Web: draw and draw-result Ops guided actions return batch execution launches instead of duplicate immediate command results.

## Non-goals

- Gate management UX.
- Cache management UX.
- New scheduler semantics beyond using the registered Spring Batch jobs as the execution path.
- Duplicating tenant-admin pages under platform routes.
