# Change: simplify-tenant-admin-pagemodel-nav

## Why

The tenant admin navigation is currently too long for the target admin V0 experience.

The web shell is driven by the backend PageModel runtime, so the simplification belongs in
`features.pagemodel` resources, not in frontend fallback navigation. The product decision is
captured in `tchalanet-server/admin_sidenav_stats_rapports.md`.

Current backend state after analysis:

- `private.dashboard.tenant_admin.template.json` resolves shell through `fileKey: private_shell_tenant_admin`.
- `PageModelJsonFragmentRegistry` maps that key to
  `pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json`.
- `private_shell_tenantadmin.json` still contains the old long sidebar:
  `Dashboard`, `tenantOverview`, `Administration`, `Jeux & ventes`, `Règles commerciales`,
  `Personnalisation`, `Rapports`.
- `tenant_admin_sidebar.json` duplicates the same older navigation but is not currently referenced by
  the template/registry. Keep it coherent or remove/deprecate it in a follow-up if confirmed unused.

## What

Simplify the tenant admin PageModel shell navigation to the compact admin V0 structure:

```text
Accueil
Vendeurs
Tirages
Contrôles
Promotions
Rapports
Plus
```

Children remain present in the PageModel payload for secondary navigation/accordion UI, but the main
sidebar must stay compact by default.

Target children:

- `Vendeurs`: Ajouter vendeur, Vendeurs actifs.
- `Tirages`: direct access to draw administration.
- `Contrôles`: Limites, Odds, Primes.
- `Promotions`: Maryaj gratis, Promotions actives.
- `Rapports`: Rapport du jour, Export / impression.
- `Plus`: Configuration générale, Mon espace, Mon compte, Support.

## Impact

Backend resources:

- Update `tchalanet-app/src/main/resources/pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json`.
- Keep `tchalanet-app/src/main/resources/pagemodel/fragments/private/tenantadmin/tenant_admin_sidebar.json`
  aligned if it remains in the repo.
- Add or align i18n keys in backend/frontend bundles only if missing after checking existing keys.
- Update tests that assert the tenant admin runtime shell, if any.

Frontend:

- No frontend navigation override should be introduced for this change.
- The frontend should continue to render the resolved `PageRuntimeResponse`.

## Non-goals

- Building the destination pages.
- Changing the PageModel runtime contract shape.
- Moving operational truth into PageModel.
- Reworking tenant readiness sections.

## Success Criteria

- `GET /tenant/dashboard` for `TENANT_ADMIN` returns a private shell with only the seven compact top-level
  admin navigation entries.
- The payload preserves child actions under the relevant parent items.
- No web fallback navigation is required to achieve the admin simplification.
