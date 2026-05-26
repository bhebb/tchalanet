# Tchalanet static PageModel fragments V1

These files are intended for the `json_file` dynamic provider.

Static fragments:
- `public_header_links.json`
- `public_footer_links.json`
- `private_sidebar_cashier.json`
- `private_cashier_quick_actions.json`
- `private_header_profile_menu.json`
- `private_footer_links.json`
- `private_sidebar_tenant_admin.json`
- `private_sidebar_platform_admin.json`

Rules:
- Header/sidenav/footer/static actions can use `json_file`.
- Dashboard widgets should use grouped dynamic sources:
  - `public_home`
  - `public_draw_results`
  - `tenant_admin_dashboard`
  - `cashier_dashboard`
  - `platform_admin_dashboard`
- Static fragments contain frontend routes, not backend API endpoints.
