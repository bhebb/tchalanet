# Web Shell

Reusable shell primitives shared by web apps.

## Owns

- public shell layout and primitives;
- private shell navigation presets and shell-owned navigation models;
- private shell layout component with app-specific utility/content projection;
- shell feedback store/outlet/banner;
- support-reference copy helpers for shell feedback;
- route-preserving shell UI utilities when they are app-independent.

## Does Not Own

- app route composition;
- app-specific runtime polling, notification loading, and session refresh wiring;
- feature routes or feature API clients;
- error copy normalization, which lives in `@tch/web/errors`;
- stateless generic UI primitives, which live in `@tch/ui/components`.

Shell feedback renders only shell-owned failures. Page, section, dialog, and field-owned API calls
must pass `suppressShellFeedback: true` and render through their local owner.
