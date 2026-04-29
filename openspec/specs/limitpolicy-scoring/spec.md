# limitpolicy-scoring Specification

## Purpose

TBD - created by archiving change fix-limitpolicy-resolver-scoring. Update Purpose after archive.

## Requirements

### Requirement: Semantique du scoring LimitResolver documentee

La méthode `LimitResolver.score()` SHALL être documentée avec la hiérarchie de spécificité des cibles. Le code SHALL inclure un commentaire explicitant l'ordre intentionnel : `Tenant < DrawChannel < Outlet < Agent < Terminal`.

#### Scenario: Terminal prime sur Agent pour la même RuleKey

- **WHEN** deux assignments actifs existent pour la même `RuleKey` — l'un ciblant le terminal du contexte, l'autre ciblant l'agent du contexte
- **THEN** l'assignment ciblant le `TerminalTarget` est sélectionné comme "best"

#### Scenario: Agent prime sur Outlet pour la même RuleKey

- **WHEN** deux assignments actifs existent pour la même `RuleKey` — l'un ciblant l'outlet du contexte, l'autre ciblant l'agent du contexte
- **THEN** l'assignment ciblant l'`AgentTarget` est sélectionné comme "best"

#### Scenario: Outlet prime sur DrawChannel pour la même RuleKey

- **WHEN** deux assignments actifs existent pour la même `RuleKey` — l'un ciblant le drawChannel du contexte, l'autre ciblant l'outlet du contexte
- **THEN** l'assignment ciblant l'`OutletTarget` est sélectionné comme "best"

#### Scenario: DrawChannel prime sur Tenant pour la même RuleKey

- **WHEN** deux assignments actifs existent pour la même `RuleKey` — l'un de niveau tenant, l'autre ciblant le drawChannel du contexte
- **THEN** l'assignment ciblant le `DrawChannelTarget` est sélectionné comme "best"

#### Scenario: Cible hors contexte ignoree

- **WHEN** un assignment cible un `TerminalTarget` qui ne correspond pas au terminal du contexte
- **THEN** cet assignment est ignoré (score = -1)
