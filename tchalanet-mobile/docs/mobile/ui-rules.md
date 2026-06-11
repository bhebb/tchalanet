# Mobile UI Rules

> Status: normative  
> Scope: `tchalanet-mobile/` Flutter UI, POS flows, and shared mobile components

This file is the entry point for mobile UI rules. Keep it short and route details to
the focused documents below:

- [01_mobile_product_rules.md](01_mobile_product_rules.md)
- [02_mobile_design_tokens.md](02_mobile_design_tokens.md)
- [03_mobile_components.md](03_mobile_components.md)
- [04_mobile_screens_v1.md](04_mobile_screens_v1.md)

## Mother Rule

POS vendeur = zero distraction.

One screen has one main task, one visible primary action, and no heavy dashboard.
The goal is not decoration. The goal is:

```text
fast • readable • reliable • usable under stress • usable on small screens
```

## Surface Split

Rules for one surface must not leak blindly into another.

| Surface | User | UI posture |
| --- | --- | --- |
| `mobile_pos` | Seller, cashier, terminal | Large touch targets, little text, direct action. |
| `mobile_admin` | Manager on phone | Summaries, fast validation, action shortcuts. |
| `web_admin` | Back-office/admin | Tables, filters, dashboards, configuration. |

The strict POS rules apply first to `mobile_pos`. `mobile_admin` may show more summary
context. `web_admin` is allowed dense tables and dashboards.

## Non-Negotiables

- No hardcoded colors in screens.
- No repeated magic sizes in screens.
- All buttons go through shared button components.
- Every POS action target is at least 44 px high.
- Primary POS action is ideally 56 px or taller.
- Red is reserved for dangerous/destructive actions.
- Green is reserved for success or OK status.
- Tchalanet navy is the primary brand role.
- Tchalanet gold is the Material 3 tertiary/accent role.
- No nested card layouts as the default structure.
- No long scrolling POS pages for critical flows.
- Screens do not contain business logic.
- View models/controllers/providers own state transitions.
- Backend remains source of truth for final sale, limits, cutoff, payout, auth, and tenant decisions.

## Project Structure

The canonical project structure and dependency direction live in
[../ARCHITECTURE.md](../ARCHITECTURE.md). Shared UI belongs in `lib/design_system/`;
feature UI belongs in `lib/features/<feature>/presentation/`.

## Screen Checklist

Before coding any screen:

- What is the primary action?
- Is it visible without scrolling?
- What is the loading state?
- What is the error state?
- What is the offline state?
- What blocks the user?
- Which button is primary?
- Which button is danger?
- Is it usable with one hand?
- Is it readable on a small screen and in bright light?
