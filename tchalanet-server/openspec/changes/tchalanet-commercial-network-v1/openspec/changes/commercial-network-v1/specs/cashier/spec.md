# Spec — Cashier feature

## ADDED Requirements

### Requirement: Cashier is UI/BFF

Cashier SHALL remain a feature layer, not a core domain. Cashier may aggregate seller profile, outlet/session status, sales summaries, warnings/notices. Cashier SHALL not own business invariants.

### Requirement: Cashier sell delegates to Sales

Cashier sell endpoint SHALL dispatch to Sales command handlers and must not create tickets directly.
