# Design: simplify-tenant-admin-pagemodel-nav

## Source Of Truth

The web consumes `PageRuntimeResponse` from `features.pagemodel`. For tenant admin, the shell is
resolved from:

```text
tchalanet-app/src/main/resources/pagemodel/templates/private.dashboard.tenant_admin.template.json
  model.shell.props.fileKey = private_shell_tenant_admin

tchalanet-features/.../PageModelJsonFragmentRegistry.java
  private_shell_tenant_admin -> pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json
```

Therefore the primary implementation is a JSON resource change in
`private_shell_tenantadmin.json`.

## Runtime Shape

Keep the existing private shell shape:

```text
topAppBar
navigationDrawer.brand
navigationDrawer.primary
navigationDrawer.sections
navigationDrawer.secondary
```

Use the compact entries as the visible navigation set. Preferred mapping:

- Put all seven compact parent entries in `navigationDrawer.primary`.
- Keep `navigationDrawer.sections` empty for this surface, so the rendered sidebar does not reintroduce
  visual section headers from the older long navigation.
- Each parent entry uses `children` for validated sub-items.

Avoid introducing new contract fields such as `collapsedByDefault` unless the frontend already consumes
them. The current web renderer opens groups only when active or manually toggled, so children are already
not expanded by default.

## Target Routes

Use stable routes from the product note, even if the pages are placeholders today:

| Entry | Route |
|---|---|
| Accueil | `/app/admin` |
| Vendeurs | `/app/admin/sellers` |
| Ajouter vendeur | `/app/admin/sellers/new` |
| Vendeurs actifs | `/app/admin/sellers?status=active` |
| Tirages | `/app/admin/draws` |
| Contrôles | `/app/admin/controls` |
| Limites | `/app/admin/controls/limits` |
| Odds | `/app/admin/controls/odds` |
| Primes | `/app/admin/controls/bonuses` |
| Promotions | `/app/admin/promotions` |
| Maryaj gratis | `/app/admin/promotions/maryaj-gratis` |
| Promotions actives | `/app/admin/promotions/active` |
| Rapports | `/app/admin/reports` |
| Rapport du jour | `/app/admin/reports/today` |
| Export / impression | `/app/admin/reports/export` |
| Plus | `/app/admin/more` |
| Configuration générale | `/app/admin/settings` |
| Mon espace | `/app/admin/more/space` |
| Mon compte | `/app/admin/more/account` |
| Support | `/app/admin/more/support` |

## Vocabulary

Use `Vendeurs` for the admin UI label, but treat it as `seller_terminal` in meaning. Do not split the
navigation into separate seller/terminal/vendor parent pages.

## Validation Notes

Do not compile unless explicitly requested. Useful checks for the next session:

- JSON parse for changed PageModel fragments/templates.
- Existing PageModel runtime assembler tests if Java is available.
- OpenSpec validation for this change.
