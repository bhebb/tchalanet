# public-runtime Specification

## Purpose
TBD - created by archiving change dashboard-overview-runtime-v1. Update Purpose after archive.
## Requirements
### Requirement: Public home PageModel uses grouped public sources

`public.home` SHALL use grouped dynamic sources and static shell fragments.

Allowed dynamic sources:

- `public_home`
- `public_draw_results`

Allowed static source:

- `json_file`

| Page | Provider source | Widgets / sections | Notes |
|---|---|---|---|
| `public.home` | `public_home` | hero, features, available games, plans/demo CTA, trust blocks | public only |
| `public.home` | `public_draw_results` | latest results, next by slot | bounded summary |
| `public.home` | static/json | check ticket prompt | action route only |
| `public.home` | `json_file` | public header/footer | routes only |

#### Scenario: Public home loads without authentication

- **GIVEN** an anonymous visitor
- **WHEN** the frontend requests public home PageModel
- **THEN** the backend resolves `public.home`
- **AND** it never requires admin/tenant/cashier authentication.

### Requirement: Public home does not preload private data

#### Scenario: Public home resolves

- **WHEN** `public.home` is resolved
- **THEN** it MUST NOT load tenant admin dashboard data
- **AND** it MUST NOT load cashier/session/sales private data
- **AND** it MUST NOT load platform admin data.

### Requirement: Public draw results page uses dedicated source

`public.draw_results` SHALL use source `public_draw_results`.

| Page | Provider source | Widgets / sections | Notes |
|---|---|---|---|
| `public.draw_results` | `public_draw_results` | latest by slot, next by slot, limited history/search state | public results page |
| `public.draw_results` | `json_file` | public header/footer | static fragments |

#### Scenario: Public draw results supports history

- **GIVEN** the visitor opens `/results`
- **WHEN** `public.draw_results` is resolved
- **THEN** the provider may include limited history according to props
- **AND** public home may reuse the same provider with history disabled.

### Requirement: Public check ticket page is action-driven

The public check ticket page MAY use PageModel for form/help content, but verification SHALL be a public API action.

#### Scenario: Visitor checks a ticket

- **GIVEN** a visitor enters a public ticket code
- **WHEN** they submit the form
- **THEN** the frontend calls the public ticket verification endpoint
- **AND** the PageModel provider is not responsible for the verification result.

