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

Shell feedback renders shell-owned failures and user-action confirmations that should remain visible
at shell level. Page, section, dialog, and field-owned API failures must pass `suppressShellFeedback:
true` and render through their local owner.

## Navigation : replié vs déployé

Une seule borne fait basculer la navigation des deux shells : **`expanded` (840px)**, exprimée par
`ui.up(expanded)` en SCSS et `TchBreakpointService.isWide()` en TS. Aucun autre palier ne doit faire
apparaître ou disparaître un moyen de navigation — voir `docs/conventions/style.md` §10.1.

Un panneau de navigation **replié est un dialogue modal** ; **déployé, c'est un panneau permanent**.
La sémantique suit le layout, elle n'est jamais figée :

| | `down(expanded)` — overlay | `up(expanded)` — permanent |
|---|---|---|
| `role` / `aria-modal` | `dialog` / `true` | absents |
| Focus piégé | oui (`ConfigurableFocusTrapFactory`) | non |
| Contenu de page `inert` | oui | non |
| Panneau `inert` quand fermé | oui (hors écran mais tabulable sinon) | sans objet |
| Scroll du document | verrouillé (`tch-overlay-open`) | libre |
| `Escape` | ferme | sans objet |
| Focus à la fermeture | rendu au déclencheur | sans objet |
| Densité `tch-sidebar-nav` | `comfortable` (cibles 48px) | `compact` |

C'est pourquoi la bascule est pilotée depuis le TS : un `role="dialog"` posé en dur resterait
annoncé aux lecteurs d'écran en desktop, où la sidebar n'est qu'un complément de page.

Côté public, la navigation mobile passe **uniquement** par le burger et `tch-overlay-nav` — il n'y a
pas de barre de navigation basse.
