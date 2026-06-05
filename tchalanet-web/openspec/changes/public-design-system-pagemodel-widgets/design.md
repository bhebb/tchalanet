# Design

## Direction

Public pages should feel like a functional portal for verification and draw consultation, not a generic promotional website.
Use Deep Blue for trust and infrastructure, and Gold/Yellow only for critical actions.

Brand values to apply to the existing base `tchalanet` theme:

- `primary`: `#1A1B4B`
- `primaryStrong`: `#15157D`
- `primaryContainer`: `#2E3192`
- `action`: `#FECB00`
- `actionHover`: `#F1C100`
- `orangeAccent`: `#F7931E` for limited secondary attention only

Angular components must consume semantic tokens, not brand colors directly.

## Material Design 3 Theme System

Material Design 3 is the public design system foundation.
The web theme must keep the existing M3 pipeline: generated tonal palettes emit `--mat-sys-*`; runtime theme code maps those system tokens into reusable Tchalanet variables consumed by public components.

Rules:

- keep Angular Material 3 as the source for color roles, elevation expectations, focus states, density, and component primitives where appropriate;
- do not introduce Tailwind theme config, CDN fonts/icons, or component-local color constants;
- expose Tchalanet component tokens as `--tch-*` variables derived from M3 tokens and first-paint fallbacks;
- support both `.tch-theme[data-preset='tchalanet']` and `.tch-theme.dark[data-preset='tchalanet']` from the first implementation;
- use dark mode tokens from M3 tonal palettes and dark runtime post-processing, not inverted light-mode colors;
- keep public widgets readable when `mode=light`, `mode=dark`, or `mode=system`.

The existing theme pipeline stays authoritative:

- edit `apps/tch-portal/src/app/core/theme/scss/tchalanet/_theme-colors.scss` by regenerating the Material tonal palette from the new brand seeds;
- keep the preset id `tchalanet` in `apps/tch-portal/src/app/core/theme/scss/theme-presets.scss`;
- update first-paint fallbacks in `apps/tch-portal/src/app/core/theme/scss/runtime-root.scss`;
- extend `apps/tch-portal/src/app/core/theme/scss/runtime-vars.scss` when a public widget needs a stable `--tch-*` variable;
- update `apps/tch-portal/src/app/core/theme/theme-token-map.ts` for backend/PageModel token overrides;
- regenerate `apps/tch-portal/src/app/core/theme/theme-presets.registry.ts` with the existing theme generator.

## Semantic Tokens

Light mode token names should match the public PageModel/theme payload where possible:

- background/surface: `background`, `surface`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceTonal`
- text: `onBackground`, `onSurface`, `onSurfaceVariant`
- borders: `outline`, `outlineVariant`
- primary: `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
- action: `secondaryContainer`, `onSecondaryContainer`, `secondaryFixedDim`
- status: `statusReady`, `statusWarning`, `statusBlocked`, `statusMissing`

Light mode target values:

```text
background              #F9F9FC
onBackground            #1A1C1E
surface                 #F9F9FC
surfaceBright           #F9F9FC
surfaceContainerLowest  #FFFFFF
surfaceContainerLow     #F3F3F6
surfaceContainer        #EDEEF1
surfaceContainerHigh    #E8E8EB
surfaceContainerHighest #E2E2E5
surfaceVariant          #E2E2E5
surfaceTonal            #EBEBF5
onSurface               #1A1C1E
onSurfaceVariant        #464652
outline                 #777683
outlineVariant          #C7C5D4
primary                 #1A1B4B
onPrimary               #FFFFFF
primaryContainer        #2E3192
onPrimaryContainer      #E1E0FF
primaryFixed            #E1E0FF
primaryFixedDim         #C0C1FF
onPrimaryFixed          #04006D
onPrimaryFixedVariant   #373A9B
secondary               #745B00
onSecondary             #FFFFFF
secondaryContainer      #FECB00
onSecondaryContainer    #241A00
secondaryFixed          #FFE08B
secondaryFixedDim       #F1C100
error                   #BA1A1A
onError                 #FFFFFF
errorContainer          #FFDAD6
onErrorContainer        #93000A
statusReady             #10B981
statusWarning           #F59E0B
statusBlocked           #DC2626
statusMissing           #64748B
```

Dark mode target behavior:

- background and surfaces come from the generated M3 dark `--mat-sys-*` roles;
- header/footer/hero chrome use dark-safe primary processing already handled in `runtime-vars.scss`;
- action yellow remains reserved for critical CTAs, with dark-mode text contrast checked through `onSecondaryContainer`;
- status colors must remain distinguishable on dark surfaces;
- all public components must keep the same token names in light and dark mode.

Usage mapping:

- page background -> `background`
- cards -> `surfaceContainerLowest`
- soft cards -> `surfaceContainerLow`
- public section bands -> `surfaceTonal`
- primary public CTA -> `secondaryContainer` + `onSecondaryContainer`
- secondary CTA -> `primary` + `onPrimary`
- hero -> `primary` + `onPrimary`

## Typography And Layout

Use `Plus Jakarta Sans` for public UI and `JetBrains Mono` for ticket/code/receipt-like data.
Replace the current global `--tch-font-family` fallback with `Plus Jakarta Sans, system-ui, sans-serif` and expose a mono token for ticket code data.

Mobile-first constraints:

- touch targets at least `48px`;
- public gutter `16px` on mobile and `32px` on desktop;
- avoid dense tables on mobile;
- use result and ticket cards for scan-friendly public states;
- use a public bottom nav with `Résultats`, `Vérifier`, `Aide`;
- do not show anonymous users private navigation such as profile links.

Buttons and standard cards use at least `8px` radius.
Large public widgets may use `12px` or `24px` radius when it improves scanning.

## Angular Component Architecture

Public implementation must follow the existing Tchalanet web architecture:

```text
Route -> Page -> Container(s) -> Component(s)
```

Roles:

- `*.page.ts`: routed component and primary screen boundary; may inject a facade, feature store, router, and runtime services;
- `*.container.ts`: never routed; orchestrates one logical sub-zone below a page and may inject a facade/store/service/router when needed;
- `*.component.ts`: visual/presentational; receives `input()`, emits `output()`, remains stateless except small UI-only signal state;
- `*.widget.ts`: PageModel-rendered public widget; receives widget props/state/dynamic payload only and should behave like a presentational component;
- `*.facade.ts`: page/feature API for loading, commands, routing intents, and store selectors;
- `*.store.ts`: explicit feature state for API-backed screen data, loading/error, filters, pagination, selected item, and view mode.

Do not route directly to a container or visual component.
Do not inject API clients, facades, NgRx store, or feature stores into visual components/widgets.

Component slicing:

- page/container components own route params, service calls, PageModel loading, i18n lookup boundaries, and navigation execution;
- widget components render props and states only;
- small shared UI primitives handle buttons, badges, status chips, result numbers, skeletons, empty states, and error notices;
- widgets do not call APIs, inspect auth state directly, mutate global theme, or know tenant/private navigation;
- widget styling uses `--tch-*` variables and Angular host classes, not inline color values.

State placement follows the project convention:

- local UI-only state such as selected tab, expanded section, input draft, or dialog open state may use component `signal()`;
- screen state such as loading/error, selected result, verification response, rules data, filters, and pagination belongs in a feature `*.store.ts`;
- reusable API cache belongs in data-access state only when multiple features need it;
- global app state such as auth, locale, theme, and shell navigation remains in `core`.

Use a facade when a page/container needs to combine store selectors, service calls, commands, navigation, or PageModel loading.
Do not create shared generic facades/stores or a global store for all public pages.

## Angular File Style And Tests

For this slice, prefer single-file standalone Angular components using inline `template` and inline `styles`.
Extract templates/styles only if a file becomes hard to maintain or an established local pattern already requires extraction.

## CSS Naming, Tokens, And Theme Safety

Public page CSS must follow the durable convention in `docs/conventions/web-public-css-theming.md`.
In short: scoped BEM-like classes, no generic unscoped class names, no component-local brand/status hex values, no Tailwind/CDN theme dependency, all themeable values through `--tch-*` variables, and browser validation across mobile/desktop plus light/dark modes.

Testing rules:

- add specs for facades, stores, containers, widgets/components with branching logic, state mapping, output behavior, form validation, i18n fallback handling, or unsafe-copy guards;
- do not add low-value specs for purely visual stateless components that only render inputs with no conditional logic beyond basic markup;
- keep visual-only validation to focused layout/browser checks where needed.

## I18n From Start

All public copy must be i18n-ready in the first implementation.
Templates should reference translation keys or PageModel-provided localized labels, not hardcoded French strings except in test fixtures and examples.

Rules:

- add public keys to the existing `apps/tch-portal/public/assets/i18n/fr.json`, `en.json`, and `ht.json`;
- define stable key namespaces such as `public.nav.*`, `public.home.*`, `public.ticket.*`, `public.results.*`, `public.rules.*`, `public.help.*`, `public.footer.*`;
- keep safe wording translations semantically equivalent across languages;
- when PageModel props provide direct text, prefer already-localized payload values but keep key fallback behavior stable;
- tests should cover at least representative French keys and fallback behavior.

## Public PageModel Contract

All public pages should be composed from reusable widgets.
Public PageModel widgets are typed contracts first, and Angular visual components second.
Every public widget contract defines:

- `type`;
- `id`;
- `props`;
- optional `state`;
- typed actions.

Actions use typed destinations:

```ts
type NavigationDestination =
  | { type: 'path'; path: string }
  | { type: 'external'; url: string }
  | { type: 'anchor'; id: string };
```

Avoid raw `href` strings inside widget props unless they are explicitly mapped at the component boundary.

Each widget receives stable props and supports these shared UI states:

```ts
type WidgetState = 'default' | 'loading' | 'empty' | 'error' | 'partial';
```

Each widget also supports mobile and desktop layout.

Actions must use typed destinations instead of unstructured href strings.
Widget business states must be explicit enums.
Widget props should use translation keys or localized labels consistently; mixed hardcoded copy in component templates is not allowed.

Domain statuses:

```ts
type VerificationStatus =
  | 'PENDING_RESULT'
  | 'NOT_PAYABLE'
  | 'PAYABLE'
  | 'INVALID_OR_CANCELLED'
  | 'NOT_FOUND'
  | 'SERVICE_UNAVAILABLE';

type ResultStatus = 'CONFIRMED' | 'PENDING' | 'UNAVAILABLE';

type SimulationStatus =
  | 'NO_GAME_SELECTED'
  | 'GAME_SELECTED'
  | 'RULES_UNAVAILABLE'
  | 'INVALID_SELECTION'
  | 'INVALID_STAKE'
  | 'SIMULATION_UNAVAILABLE'
  | 'CALCULATED';
```

Primary widget set:

- `PublicHeaderWidget`
- `HeroWidget`
- `TicketVerificationWidget`
- `VerificationResultWidget`
- `LatestResultsWidget`
- `ResultCardWidget`
- `ResultDetailWidget`
- `HowItWorksWidget`
- `RulesWidget`
- `SimulationWidget`
- `OperatorCtaWidget`
- `NewsListWidget`
- `NewsDetailWidget`
- `RelatedNewsWidget`
- `NewsletterWidget`
- `HelpFaqWidget`
- `TrustWidget`
- `FooterWidget`
- `PublicBottomNavWidget`

## Public Routes

V1 routes:

- `/public`
- `/public/check-ticket`
- `/public/results`
- `/public/results/:id`
- `/public/rules`
- `/public/help`
- `/public/contact`
- `/public/privacy`
- `/public/terms`

V1.5 routes:

- `/public/news`
- `/public/news/:slug`
- `/public/operators`
- `/public/status`
- `/public/games`

Scope split:

- V1 prioritizes the public design system, anonymous-safe navigation, first public pages, typed widget contracts, and safe PageModel-compatible composition.
- Do not block V1 delivery on the full rules/simulation implementation or news implementation.
- `/public/rules` may ship initially as a PageModel-compatible rules/simulation surface with unavailable/empty states and no frontend payout computation.
- Full news list/detail and complete rules/simulation data integration remain V1.5 unless backend/PageModel payloads are already available.

## Safe Wording

Public copy must prefer cautious wording:

- `Sources prises en charge`
- `Résultats confirmés`
- `Statut du résultat`
- `Ticket vérifiable`
- `Point de vente participant`
- `Vérification publique du ticket`
- `Simulation indicative`
- `Gain estimé indicatif`
- `Ticket payable`
- `Ticket non payable`
- `En attente de résultat`
- `Service temporairement indisponible`

Avoid claims such as official certification, approved operators, guarantees, certified results, secure payment guarantees, or direct payment by Tchalanet unless backed by an explicit legal/product decision.

Simulation disclaimer:

> Simulation indicative. Le montant payable dépend du ticket réel, des règles du point de vente, du statut du tirage et des résultats confirmés. Cette simulation ne garantit pas un paiement.

## Core Widgets

### TicketVerificationWidget

Primary route: `/public/check-ticket`.

The widget verifies a ticket by public code or QR scan.

Required copy:

- title: `Vérifier un ticket`
- description: `Entrez votre code public pour consulter le statut de votre ticket.`
- label: `Code public du ticket`
- placeholder: `ABC-123-XYZ`
- CTA: `Vérifier maintenant`
- helper: `Où trouver le code public ?`

Business states:

- `PENDING_RESULT`
- `NOT_PAYABLE`
- `PAYABLE`
- `INVALID_OR_CANCELLED`
- `NOT_FOUND`
- `SERVICE_UNAVAILABLE`

Do not use winning/gambling phrasing such as "savoir si vous avez gagné", "GANANT - WINNER", or "Tentez votre chance".

### LatestResultsWidget And ResultCardWidget

Primary route: `/public/results`.

Each card shows game name, draw date/time, result status, numbers, last update, and `Voir détail`.

Result statuses:

- `CONFIRMED` -> `Résultats confirmés`
- `PENDING` -> `En attente`
- `UNAVAILABLE` -> `Indisponible`

Recommended subtitle:

`Résultats confirmés selon les sources prises en charge.`

### ResultDetailWidget

Primary route: `/public/results/:id`.

Show game name, draw date/time, status, numbers, supported source, last update, related results if available, and a `Vérifier un ticket` CTA.

### RulesWidget And SimulationWidget

Primary route: `/public/rules`.

The page is educational and indicative only.
Multipliers, odds, pricing, and payout calculations must come from a backend API, PageModel payload, or tenant-aware catalog pricing.

Required page copy:

- title: `Règles des jeux et simulation`
- subtitle: `Consultez les règles des jeux et estimez un gain indicatif selon une mise d'exemple.`
- simulation badge: `Indicatif`
- output label: `Gain estimé indicatif`

Rules content:

- game selector;
- how to play;
- valid selections;
- examples;
- draw dependency;
- unavailable rules state.

Simulation content:

- game;
- selection;
- stake;
- estimated payout;
- validation errors;
- always-visible disclaimer.

Simulation states:

- `NO_GAME_SELECTED`
- `GAME_SELECTED`
- `RULES_UNAVAILABLE`
- `INVALID_SELECTION`
- `INVALID_STAKE`
- `SIMULATION_UNAVAILABLE`
- `CALCULATED`

Simulation display rules:

- never compute payouts, odds, multipliers, or game pricing from hardcoded frontend values;
- display only values returned by backend/API/PageModel payload, validation errors, or unavailable states;
- if simulation data is missing, show `La simulation est temporairement indisponible pour ce jeu.`;
- always display the simulation disclaimer.

### OperatorCtaWidget

Use:

`Vous gérez un point de vente ou un réseau de vendeurs ?`

`Découvrez comment Tchalanet peut simplifier votre gestion quotidienne, mieux suivre vos ventes et automatiser vos rapports d'activité.`

Actions:

- `Demander une démo`
- `Accès opérateur`, or `En savoir plus` if operator access is not available

## Header And Footer

Desktop public navigation:

- `Tchalanet`
- `Résultats`
- `Vérifier un ticket`
- `Aide`
- `Pour opérateurs`
- `Connexion`

Public mobile bottom navigation:

- `Résultats`
- `Vérifier`
- `Aide`

Do not show public anonymous users `Profile`, English `News`, `Draws`, `Results`, `Login`, or `Banque de Borlette`.
Also do not show private dashboard links, admin navigation, cashier navigation, or tenant-admin actions on public anonymous pages.

Footer copy:

`Infrastructure digitale pour la gestion professionnelle des tickets, tirages et opérations de loterie.`

Footer columns:

- Solutions: `Pour opérateurs`, `Vérification de tickets`, `Gestion points de vente`
- Support: `Aide`, `Résultats`, `État du système`, `Contact`
- Legal: `Confidentialité`, `Conditions d'utilisation`, `Conformité`

Status label:

`Systèmes opérationnels`

## Stitch Mockup Adaptation Rules

The provided Stitch HTML is a visual direction reference only.
Adapt the public structure and interactions, but correct unsafe or off-scope content:

- replace `primary: #15157D` with official `primary: #1A1B4B`, keeping `#15157D` only as `primaryStrong` if needed;
- remove `WINNER`, jackpot amounts, remote barcode/news images, and lottery brand examples that imply unsupported official data;
- replace `Tirages` in the mobile nav with `Résultats`;
- keep `Connexion` in French, not `Login`;
- use local icon components/libraries instead of Google Material Symbols CDN;
- use Angular styles and `--tch-*` variables instead of Tailwind config colors;
- keep the ticket visual as `Ticket vérifiable` or `Statut du ticket`, never winner language;
- add the missing Rules/Simulation page surface instead of only showing home sections.
