# Spec: Dashboard and Overview E2E

## ADDED Requirements

### Requirement: Dashboards use expected grouped providers

Tenant admin uses `tenant_admin_dashboard`, cashier web uses `cashier_dashboard`, platform admin uses `platform_admin_dashboard`.

### Requirement: Overviews are not dashboards

Tenant and platform overviews SHALL return structural sections and SHALL NOT repeat dashboard KPI fields.
