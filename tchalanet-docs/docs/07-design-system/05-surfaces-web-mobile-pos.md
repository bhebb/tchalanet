# 05 Surfaces — Web, Mobile, POS

> Status: normative

## PUBLIC_LANDING

Web shell:

```text
PublicHeader + main content + PublicFooter
```

Allowed:

- hero
- feature cards
- promotional cards
- plan/pricing cards
- contact block
- testimonials
- footer

CTA rules:

- `Demander une démo` uses `tertiary` orange.
- Secondary CTA uses primary outline/text.
- Landing may use decorative gradients and stronger visuals.

Forbidden:

- dense POS transactional widgets as main content;
- admin tables;
- cashier operational workflows.

## MOBILE_CASHIER

Allowed:

- cashier identity
- session summary
- main actions
- recent activity
- dashboard cards
- bottom nav

Rules:

- mobile may show more context than POS;
- scroll is allowed but should remain moderate;
- use progressive disclosure for details.

## POS_TERMINAL

Allowed:

- terminal header
- large action cards
- sync status
- daily totals
- last transaction
- bottom navigation

Forbidden:

- marketing sections;
- long landing layout;
- dense admin tables;
- tiny action buttons;
- unclear sync/offline state.

## TENANT_ADMIN_WEB

Private Web shell:

```text
PrivateTopAppBar + SidebarNav + main content
```

Allowed:

- side navigation
- top header
- filters
- tables
- paginated lists
- dashboards
- detail pages

Rules:

- higher density is allowed;
- destructive actions require confirmation;
- tables must be paginated and responsive.

## Shell and PageModel boundary

`PageModel` renders content only: rows, columns, and widgets. Header, footer, top app bar, sidebar,
theme bootstrap, and runtime file resolution belong outside PageModel.

Mobile/POS visual alignment is planned as a separate Mobile-owned change. This document defines the
target roles without claiming that Flutter is already migrated.
