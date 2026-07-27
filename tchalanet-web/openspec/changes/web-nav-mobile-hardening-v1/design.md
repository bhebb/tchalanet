# Design — Web Nav & Mobile Hardening

## 0. Le contrat de bascule

Une seule frontière sépare « navigation repliée » de « navigation déployée » :

```
compact    < 600px    →  drawer / overlay        nav masquée dans le header
medium     600–839px  →  drawer / overlay        nav masquée dans le header
expanded   ≥ 840px    →  sidebar persistante     nav inline dans le header
large      ≥ 1200px   →  (densité seulement)
extra-large ≥ 1600px  →  (densité seulement)
```

**840px = `up(expanded)`.** C'est la seule valeur qui pilote une bascule de
navigation, dans les deux shells, en SCSS comme en TS.

| Couche | Expression |
|---|---|
| SCSS | `@include ui.up(expanded)` / `@include ui.down(expanded)` |
| TS | `TchBreakpointService.isWide()` (= expanded ∪ large ∪ extraLarge) |

`TchBreakpointService` (`libs/ui/components/src/lib/breakpoints/`) implémente déjà
exactement ces bornes et se dit explicitement synchronisé avec `$bps` de
`_breakpoints.scss`. Le contrat consiste à **s'en servir**, pas à en créer un autre.

Les paliers `medium`, `large` et `extra-large` restent utilisables pour la densité
(taille de police, padding, nombre de colonnes). Ils ne doivent jamais faire
apparaître ou disparaître un moyen de navigation.

### Pourquoi 840 et pas 768

768 est la valeur iPad historique ; 840 est la borne M3 `expanded`, celle que la
convention du repo a déjà choisie et que `TchBreakpointService` code en dur.
Prendre 768 obligerait à désaligner le TS du SCSS. En pratique la bascule recule
donc de 72px : sur un iPad portrait (768×1024) la nav publique passe du mode
inline au mode burger. C'est le comportement M3 attendu et cohérent avec le shell
privé, qui montre déjà son drawer en overlay jusqu'à 720px.

## 1. Sémantique du drawer / overlay

Un panneau de navigation replié est un **dialogue modal** ; déployé, c'est un
**complément de page**. La sémantique doit suivre le layout, pas être figée.

| | `down(expanded)` — overlay | `up(expanded)` — persistant |
|---|---|---|
| `role` | `dialog` | aucun (`<aside>` / `<nav>`) |
| `aria-modal` | `true` | absent |
| Focus trap | oui | non |
| `inert` sur le contenu | oui | non |
| Scroll-lock du body | oui | non |
| `Escape` ferme | oui | sans objet |
| Focus rendu au déclencheur | oui | sans objet |

D'où le pilotage par `TchBreakpointService.isWide()` plutôt que par media query
CSS seule : un `role="dialog"` posé en dur resterait annoncé aux lecteurs d'écran
en desktop, où la sidebar est un simple panneau permanent.

Le focus trap s'appuie sur `ConfigurableFocusTrapFactory` (`@angular/cdk/a11y`).
Le CDK est déjà une dépendance (`@angular/cdk/layout` dans `TchBreakpointService`,
Angular Material partout) — pas de dépendance nouvelle.

## 2. État actif et technologies d'assistance

La logique d'activité **existe déjà** dans les deux composants de nav ; elle ne
produit qu'une classe CSS.

- `tch-nav` : `routerLinkActive="is-active"` → ajouter `ariaCurrentWhenActive="page"`,
  l'entrée officielle d'Angular pour ça.
- `tch-sidebar-nav` : `isActionActive(item)` alimente `[class.is-active]` →
  alimenter aussi `[attr.aria-current]`. Aucune logique nouvelle à écrire, le
  calcul (alias `activeRoutes`, `activeMatch: 'exact'`, query params) reste intact.

Le toggle de groupe (`.sidebar__group-toggle`) porte déjà `aria-expanded` :
correct, il n'annonce pas une page mais un dépliage.

## 3. Cibles tactiles

style.md §14 : 44px minimum, 48px préféré, token `--tch-touch-target`.

| Élément | Actuel | Cible |
|---|---|---|
| `tch-sidebar-nav` item racine | 2.75rem (44px) | conforme, inchangé |
| `tch-sidebar-nav` `.sidebar__child` | 2.25rem (36px) | 48px **en mode overlay** ; densité actuelle conservée sur sidebar persistante |
| `tch-overlay-nav` lien | `padding: .875rem` seul | `min-height: var(--tch-touch-target)` |
| `admin-list-surface` | `--tch-size-touch-target` (token inexistant) | `--tch-touch-target` |

La distinction overlay/persistant sur les enfants de sidebar est délibérée : en
desktop la sidebar est parcourue à la souris et la densité a de la valeur (le
menu platform compte 9 groupes) ; en mobile elle est parcourue au doigt.

## 4. Garde-fou

Corriger 18 fichiers sans garde-fou ne fait que remettre le compteur à zéro : la
dérive vient de ce que rien n'empêche d'écrire `@media (max-width: 823px)`.

Règle : **une valeur de breakpoint littérale dans un `@media` est une erreur**,
sauf dans `_breakpoints.scss`. Le mécanisme (stylelint si le workspace en a un,
sinon test Vitest de conformité sur les fichiers en périmètre) est tranché à
l'implémentation selon l'outillage réellement présent — le contrat, lui, est que
la règle échoue en CI.

Portée initiale : les répertoires en périmètre. L'élargissement progressif au
reste du workspace se fait ensuite fichier par fichier, sans big-bang.

## 5. Ce que ce change ne touche pas

- Le contenu et la hiérarchie de `PLATFORM_NAVIGATION` / `TENANT_ADMIN_NAVIGATION` :
  aucun item ajouté, retiré ou déplacé.
- Les routes et les guards.
- Le rendu PageModel et les widgets publics.
- Les tables des consoles (chantier C2) — un `overflow-x: auto` par table reste
  la situation actuelle, non traitée ici.
