# 01 Mobile Product Rules

> Status: normative  
> Scope: product behavior and information density for Tchalanet mobile surfaces

## Product Principle

POS vendeur = zero distraction.

The POS is a work tool used under pressure. It must optimize for speed, legibility, and
confidence. It is not a marketing surface and not an analytics dashboard.

## Surface Rules

### `mobile_pos`

For sellers, cashier use, and terminal-like workflows.

- One primary task per screen.
- One primary action visible.
- Large buttons.
- Short labels.
- Minimal text.
- No heavy dashboard.
- No long scrolling pages in critical flows.
- Sticky action area for final actions.

### `mobile_admin`

For lightweight manager actions on mobile.

- Summary first.
- Action shortcuts second.
- Approval/validation flows should be fast and explicit.
- More context is allowed than POS, but it must remain scan-friendly.

### `web_admin`

For configuration, reporting, operations, and supervision.

- Tables, filters, dashboards, and dense information are acceptable.
- Do not force POS large-button patterns onto web admin.
- Do not use web admin density on POS.

## Navigation V1

```text
Login
  -> Accueil vendeur
  -> Vente ticket
  -> Preview / Verification
  -> Succes vente
  -> Nouveau ticket ou Tickets recents
```

Do not introduce broad free navigation at the beginning of POS V1.

POS layout shape:

```text
compact header
content
sticky action bar
```

No heavy bottom navigation for V1.

## UX Copy Rules

Always say what to do. Avoid backend terms.

Bad:

```text
Validation failed
Operational context invalid
```

Good:

```text
Impossible de vendre ce ticket.
La mise depasse la limite autorisee.

Ce terminal n'est pas pret pour vendre.
Verifiez la session ou contactez l'administrateur.
```

POS copy rules:

- Short sentences.
- One instruction at a time.
- No raw backend enum names.
- No vague "Erreur" without next action.

## Required States

Every POS feature must handle:

- Loading profile/session/draws.
- Offline mode.
- Closed session.
- Invalid terminal.
- Blocked outlet.
- Empty cart.
- Validation warning.
- Validation rejected.
- Network error.

These states are part of the product, not edge decorations.
