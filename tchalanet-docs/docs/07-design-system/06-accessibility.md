# 06 Accessibility

> Status: normative

## Minimum rules

- Touch targets are at least 44 px.
- Focus must be visible.
- Critical status must not rely on color alone.
- Error messages must be textual.
- Disabled state must be visually distinct.
- POS text must remain readable under glare and on low-end screens.

## POS accessibility

POS screens must account for:

- fast repeated usage;
- one-handed or countertop usage;
- low attention during busy sales;
- unreliable internet states;
- older Android devices.

## Contrast

Rules:

- `onPrimary` on `primary` must be readable.
- Use `#FFFFFF` for text/icons on critical primary buttons.
- `#F2F3F5` is allowed as `onPrimarySoft` on large surfaces only.
- Secondary muted labels must remain readable on `surfaceBright` and `surface`.

## Keyboard / scanner

Web/POS with keyboard or scanner must preserve:

```text
focus ring
tab order
visible active field
enter/confirm behavior
```
