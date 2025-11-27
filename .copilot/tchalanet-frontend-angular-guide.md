# Tchalanet – Frontend Angular Guide (for Copilot)

> This file describes how Angular code should be structured and styled for the Tchalanet project.

---

## 1. Tech Stack & Constraints

- **Framework**: Angular 20+
- **Workspace**: Nx Monorepo
- **UI Library**: Angular Material (MDC)
- **Styling**: SCSS + CSS Custom Properties (design tokens)
- **State**: Signals + RxJS (where needed)
- **Routing**: Angular Router, lazy-loaded feature modules
- **i18n**: JSON translation files (`fr`, `en`, `ht`), functional namespaces (`nav.*`, `footer.section.*`, etc.)

---

## 2. High-Level Architecture

- Use Nx libraries to separate concerns:

  - `feature/*` – smart feature modules (pages, flows).
  - `ui/*` – reusable presentational components (buttons, cards, widgets).
  - `data-access/*` – services, API clients, facades.
  - `util/*` – pure helpers.

- Public vs Private:

  - Public app: marketing site, public page model, ticket verification.
  - Private app: dashboards (super admin, tenant admin, vendor).

- For the **public home**, data comes from a backend **PageModel**:

  ```ts
  interface PageModel {
    currentLang: string;
    langs: string[];
    theme: { presetId: string; mode: 'light'|'dark'|'system'; };
    header: HeaderConfig;
    footer: FooterConfig;
    layout: LayoutConfig;
    widgets: WidgetConfig[];
  }
  ```

  Components should be built to consume such a model – not hardcode content.

---

## 3. Angular Coding Style

- Use **standalone components** when appropriate, but can also rely on NgModules in feature libs.
- Always set `changeDetection: ChangeDetectionStrategy.OnPush`.
- Prefer **signals** over `BehaviorSubject` for UI state:

  ```ts
  readonly vm = computed(() => ({
    loading: this.loading(),
    items: this.items(),
  }));
  ```

- Use new Angular control flow:

  ```html
  @if (vm().loading) {
    <tch-spinner />
  } @else {
    @for (item of vm().items; track item.id) {
      <tch-card [item]="item" />
    }
  }
  ```

- Components should be **presentational** when possible:
  - Inputs are strongly typed.
  - Outputs are `EventEmitter` used to bubble events upward.

---

## 4. Theming & Design Tokens

- No hard-coded colors in components.
- Use CSS variables defined at root / theme level, for example:

  ```scss
  :host {
    color: var(--tch-text-primary);
    background-color: var(--tch-surface-header);
  }
  ```

- Theme comes from backend as a **preset** + optional overrides.
- Angular app should expose a service like `ThemeService` or `ThemeDomApplier` to:
  - read the theme from PageModel
  - apply CSS vars to `:root` or a host element.

---

## 5. Layout & Responsiveness

- Mobile-first design.
- Use three main breakpoints: 480px, 768px, 1024px.
- Use CSS Flex/Grid, not tables, for layout.
- Use Angular Material layout elements where appropriate, but custom SCSS for brand-specific styling.

- Header public rules (short version):
  - Mobile: brand left, avatar/burger right, L2 toolbar with CTA + icons (search, language, theme).
  - Tablet: brand left, nav center, account right, L2 for actions.
  - Desktop: brand → nav → search → language → theme → CTA.

---

## 6. Data Access & APIs

- Create API services in `data-access/*` libs, not directly in feature components.
- Services use `HttpClient` and return `Observable<T>` or convert to signals via adapters.
- For public page:

  ```ts
  getPublicPageModel(): Observable<PageModel> {
    return this.http.get<PageModel>('/api/public/page-model?v=1');
  }
  ```

- For authenticated dashboards, use interceptors for auth tokens.

---

## 7. Widget Renderer

- The public home uses a **WidgetRenderer** component to render sections based on config:

  ```ts
  type WidgetConfig =
    | { type: 'HeroWidget';  props: HeroProps; }
    | { type: 'DrawsWidget'; props: DrawsProps; }
    | { type: 'NewsWidget';  props: NewsProps; }
    | { type: 'TchalaWidget'; props: TchalaProps; }
    | { type: 'PricingWidget'; props: PricingProps; };
  ```

- WidgetRenderer selects component via `ngSwitch` or mapping:

  ```ts
  const registry: Record<string, Type<WidgetComponent>> = {
    HeroWidget: HeroWidgetComponent,
    DrawsWidget: DrawsWidgetComponent,
    // ...
  };
  ```

- Widgets should be small, focused, easily testable.

---

## 8. Accessibility & i18n

- All interactive elements (buttons, links, icons) must have proper ARIA labels.
- Ensure contrast ratios meet WCAG AA.
- Use translation keys with functional namespaces:

  - `nav.home`, `nav.pricing`, `footer.section.legal`, `cta.request_demo`, etc.

- Avoid hard-coded strings in components; use translation service:

  ```html
  {{ 'nav.home' | translate }}
  ```

---

## 9. Testing

- Use Jest or Karma (depending on project) for unit tests of components & services.
- Components:
  - test input/output, DOM changes, conditionals (`@if`, `@for`).
- Services:
  - test HTTP calls with Angular `HttpTestingController`.

---

## 10. Summary for Copilot

When generating Angular code for Tchalanet:

1. Use **OnPush** components, signals, and new Angular templates (@if/@for).
2. Place feature logic in `feature/*` libs, reuse visual components from `ui/*`.
3. Consume backend PageModel API for the public site instead of hardcoding layout/data.
4. Respect theming & CSS vars: no hard-coded colors or typography.
5. Keep components small, composable, and accessible.
6. Generate code that looks like it belongs to a modern, mobile-first Angular 20 + Material app.