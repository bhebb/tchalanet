## ADDED Requirements

### Requirement: evaluate() utilise les cibles du contexte

`LimitPolicyRuntimeService.evaluate(LimitContext)` SHALL charger les assignments actifs correspondant aux cibles présentes dans le `LimitContext` (tenant, outlet, agent, terminal, drawChannel). L'appel avec une liste vide de cibles est interdit sauf si le contexte ne contient aucun identifiant de cible.

#### Scenario: Évaluation avec agent et outlet dans le contexte

- **WHEN** `evaluate(ctx)` est appelé avec un `ctx` contenant `tenantId`, `agentId`, `outletId`
- **THEN** `listActiveForTargets` est appelé avec au minimum 3 cibles : `[Tenant(tenantId), Agent(agentId), Outlet(outletId)]`

#### Scenario: Évaluation avec seulement tenantId

- **WHEN** `evaluate(ctx)` est appelé avec un `ctx` contenant seulement `tenantId` (autres IDs null)
- **THEN** `listActiveForTargets` est appelé avec `[Tenant(tenantId)]`

### Requirement: LimitContext expose ses cibles

`LimitContext` SHALL fournir une méthode `toTargets()` retournant la liste des `LimitTarget` non-null correspondant aux identifiants présents dans le record. `tenantId` est toujours inclus.

#### Scenario: toTargets avec tous les IDs présents

- **WHEN** `LimitContext` contient `tenantId`, `agentId`, `terminalId`, `outletId`, `drawChannelId`
- **THEN** `toTargets()` retourne une liste de 5 éléments couvrant tous les niveaux

#### Scenario: toTargets avec IDs partiels

- **WHEN** `LimitContext` contient `tenantId` et `agentId` seulement (autres null)
- **THEN** `toTargets()` retourne `[Tenant(tenantId), Agent(agentId)]`
