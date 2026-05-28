# static-fragments Specification

## Purpose
TBD - created by archiving change dashboard-overview-runtime-v1. Update Purpose after archive.
## Requirements
### Requirement: Static fragments are route-only UI descriptors

Static fragments loaded by `json_file` SHALL contain frontend routes and labels, not backend endpoints and not dynamic data.

Allowed files V1:

| File key | Purpose |
|---|---|
| `public_header_links` | public header navigation |
| `public_footer_links` | public footer navigation |
| `private_sidebar_cashier` | cashier web sidenav |
| `private_cashier_quick_actions` | cashier quick actions |
| `private_header_profile_menu` | logged-in profile menu |
| `private_footer_links` | generic private footer |
| `private_sidebar_tenant_admin` | tenant admin sidenav |
| `private_sidebar_platform_admin` | platform admin sidenav |

### Requirement: Static fragments do not contain live dashboard payloads

Static fragments SHALL NOT contain KPI, readiness, tenant-specific state, session state, or live counts.

### Requirement: Static routes are frontend routes

Static paths SHOULD use frontend route conventions such as:

- `/app/admin/...`
- `/app/cashier/...`
- `/app/platform/...`
- `/results`
- `/verifier`

They SHOULD NOT use backend API paths such as `/api/v1/admin/...`.

### Requirement: json_file provider validates file_key

The `json_file` provider SHALL reject unknown file keys or return a controlled `dynamic.error`.

