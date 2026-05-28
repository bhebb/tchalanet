# Spec — PageModel Security

## ADDED Requirements

### Requirement: Public PageModels expose only public-safe content

Public PageModels must not expose private operational/admin/superadmin content.

### Scenario: anonymous user loads public home
Given an anonymous user
When the user requests the public home PageModel
Then the response is 200
And the response contains only public-safe widgets
And the response contains no private/admin/platform routes
And the response contains no operational context

### Requirement: Private PageModel is resolved from server context

The server must resolve the concrete private PageModel using `TchRequestContext`.

### Scenario: cashier requests own dashboard
Given an authenticated user with authority CASHIER
When the user requests `/private/page-model/dashboard`
Then the server returns `private.dashboard.cashier.web`

### Scenario: tenant admin requests own dashboard
Given an authenticated user with authority TENANT_ADMIN
When the user requests `/private/page-model/dashboard`
Then the server returns `private.dashboard.tenant_admin`

### Scenario: superadmin requests own dashboard
Given an authenticated user with authority SUPER_ADMIN
When the user requests `/private/page-model/dashboard`
Then the server returns `private.dashboard.superadmin`

### Requirement: Unauthorized private PageModel access is forbidden

### Scenario: cashier attempts tenant admin dashboard
Given an authenticated user with authority CASHIER
When the user attempts to request `private.dashboard.tenant_admin`
Then the server returns 403
And no dynamic tenant admin provider is invoked

### Scenario: tenant admin attempts superadmin dashboard
Given an authenticated user with authority TENANT_ADMIN
When the user attempts to request `private.dashboard.superadmin`
Then the server returns 403
And no platform admin dashboard provider is invoked

### Requirement: Dynamic providers revalidate access

Sensitive dynamic providers must revalidate access.

### Scenario: cashier attempts platform provider
Given an authenticated user with authority CASHIER
When a platform admin dashboard provider is invoked
Then the provider rejects the call with 403

### Requirement: No silent fallback

Unauthorized access must not return another PageModel.

### Scenario: wrong private page requested
Given a cashier requests a tenant admin page
When access is denied
Then the response is 403
And the response is not replaced by cashier dashboard
