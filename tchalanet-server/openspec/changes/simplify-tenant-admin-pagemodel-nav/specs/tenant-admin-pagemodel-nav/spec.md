# tenant-admin-pagemodel-nav

## ADDED Requirements

### Requirement: Tenant Admin Navigation Is Compact

The tenant admin PageModel runtime shell SHALL expose a compact top-level admin navigation containing:

- Accueil
- Vendeurs
- Tirages
- Contrôles
- Promotions
- Rapports
- Plus

#### Scenario: TENANT_ADMIN opens dashboard

- **WHEN** a TENANT_ADMIN requests `GET /tenant/dashboard`
- **THEN** the resolved private shell navigation contains the seven compact top-level entries
- **AND** it does not expose the old long sidebar groups as top-level navigation.

### Requirement: Tenant Admin Navigation Keeps Secondary Actions As Children

The compact tenant admin navigation SHALL keep validated secondary actions as child entries under the
corresponding parent.

#### Scenario: Admin opens Vendeurs group

- **WHEN** the frontend renders the `Vendeurs` navigation parent
- **THEN** the PageModel payload includes child entries for `Ajouter vendeur` and `Vendeurs actifs`.

#### Scenario: Admin opens Plus group

- **WHEN** the frontend renders the `Plus` navigation parent
- **THEN** the PageModel payload includes child entries for configuration, workspace, account, and support.
