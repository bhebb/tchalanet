# Design

## Shared primitive

`TchFeedbackFocusDirective` is a standalone directive selected by `[tchFeedbackFocus]`.

- `tchFeedbackFocus` controls whether the owner requests focus;
- `tchFeedbackFocusKey` retriggers the behavior when a persistent surface receives new content;
- the directive waits until Angular has rendered, scrolls the host to the center of the viewport,
  then calls `focus({ preventScroll: true })`;
- it provides `tabindex=-1` while active and does not choose the owner or severity.

The directive is deliberately presentation-agnostic. The host component remains responsible for
`role="alert"` versus `role="status"` and for the localized message.

## Ownership policy

| Surface | Default behavior |
| --- | --- |
| Page-blocking error | focus the page error surface and offer retry |
| Form summary | focus after unconsumed violations change |
| Field error | keep focus on the edited field; do not scroll a global surface |
| Action success | focus the local success notice when it is rendered |
| Section degradation/info/warning | render locally without automatic focus |
| Shell feedback | focus only when the shell owns the failure |

Feature pages must not use `viewChild`, `requestAnimationFrame`, or a local scroll/focus helper for
these cases.
