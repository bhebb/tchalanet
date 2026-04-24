---
name: frontend-web
description: >
  Déclencher pour tout code Angular, Nx, NgRx, composants, pages, widgets,
  styles SCSS, i18n, ou tests Vitest/Playwright dans tchalanet-web ou libs/.
  Indispensable si la tâche concerne : standalone components, signals, NgRx store,
  theming CSS variables, @ngx-translate, Nx generators, ou Playwright E2E.
---

# Frontend Web — Tchalanet

> Source de vérité : `apps/tchalanet-web/` et `libs/`

## Stack

| Item            | Valeur                                     |
| --------------- | ------------------------------------------ |
| Node            | 20.19.x                                    |
| Package manager | pnpm 10.19.0 (corepack)                    |
| Build tool      | Nx 21.4.1 + Vite 6                         |
| Framework       | Angular 20.2.4                             |
| UI              | Angular Material 20.2.x                    |
| State           | NgRx (store + effects + router-store)      |
| Styling         | SCSS + CSS variables (token-based theming) |
| i18n            | @ngx-translate (fr / en / ht)              |
| Tests unitaires | Vitest                                     |
| Tests E2E       | Playwright                                 |

## Architecture Nx

```
apps/tchalanet-web/     ← application Angular (routing, bootstrap uniquement)
libs/shared/            ← auth, API, state, config, types, analytics
libs/ui/                ← composants réutilisables, theming, styles
libs/web/               ← pages, widgets, shell (web-only)
libs/i18n/              ← traductions (fr, en, ht)
```

**Règle** : toute logique réutilisable va dans `libs/`, jamais dans `apps/`.

## Composants — règles absolues

```typescript
// ✅ Standalone uniquement — jamais NgModule
@Component({
  standalone: true,
  imports: [CommonModule, MatButtonModule, TranslateModule],
  template: `
    @if (isLoading()) {
      <mat-spinner />
    } @else {
      @for (item of items(); track item.id) {
        <app-ticket-card [ticket]="item" />
      }
    }
  `
})
export class TicketListComponent {
  // ✅ Signaux préférés aux observables pour le state local
  isLoading = signal(false);
  items = signal<TicketSummary[]>([]);
}

// ❌ Jamais
@NgModule({ declarations: [...] })          // NgModule interdit
*ngIf="condition"                           // ancienne syntaxe
*ngFor="let item of items"                  // ancienne syntaxe
```

## Control flow moderne (obligatoire)

```html
@if (condition) { ... } @else { ... } @for (item of items; track item.id) { ... } @switch (status) {
@case ('ACTIVE') { ... } } @defer { <heavy-component /> }
```

## Signaux — quand les utiliser

```typescript
// ✅ State local du composant → signals
count = signal(0);
doubled = computed(() => this.count() * 2);
effect(() => console.log(this.count()));

// ✅ State global partagé → NgRx store
// ✅ Async HTTP → NgRx effects + toSignal()
results = toSignal(this.store.select(selectTickets), { initialValue: [] });
```

## NgRx — structure par feature

```
libs/shared/src/lib/store/
└─ ticket/
   ├─ ticket.actions.ts
   ├─ ticket.reducer.ts
   ├─ ticket.effects.ts
   ├─ ticket.selectors.ts
   └─ ticket.facade.ts     ← isole les composants du store
```

```typescript
// ✅ Les composants passent toujours par la façade
@Component({ ... })
export class TicketListComponent {
  constructor(private facade: TicketFacade) {}
  tickets = toSignal(this.facade.tickets$);
}
```

## Theming et styles

```scss
// ✅ Jamais de couleur hardcodée
color: var(--tch-color-primary);
background: var(--tch-surface-variant);

// ❌ Interdit
color: #3f51b5;
background: rgb(255, 255, 255);
```

```scss
// Variables CSS définies dans libs/ui/src/lib/theming/
// Override par tenant via class sur <body>
body.tenant-abc {
  --tch-color-primary: #e63946;
}
```

Mobile-first : breakpoints 480 / 768 / 1024.

## i18n — @ngx-translate

```typescript
// ✅ Clés snake_case avec namespaces
// fichier : libs/i18n/src/assets/fr.json
{
  "ticket": {
    "list_title": "Mes tickets",
    "status_active": "Actif",
    "action_buy": "Acheter"
  }
}

// ✅ Dans le template
{{ 'ticket.list_title' | translate }}

// ❌ Jamais de string hardcodée dans les templates
<h1>Mes tickets</h1>  // interdit
```

3 locales obligatoires : `fr`, `en`, `ht`.

## Nx — commandes utiles

```bash
# Générer un composant standalone
nx g @angular/core:component ticket-card --project=ui --standalone

# Générer une lib
nx g @nx/angular:library my-lib --directory=libs/my-lib

# Servir
nx serve tchalanet-web

# Tests
nx test tchalanet-web
nx e2e tchalanet-web-e2e

# Build
nx build tchalanet-web --configuration=production

# Voir le graphe de dépendances
nx graph

# Affecter uniquement les projets touchés
nx affected:test
nx affected:build
```

## Widgets vs Pages

```
libs/web/src/lib/
├─ pages/      ← routing, layout, orchestration (thin)
└─ widgets/    ← blocs UI autonomes avec leur propre state local
```

- Pages → orchestrent les widgets, pas de logique UI directe
- Widgets → autonomes, réutilisables, pas de logique dupliquée entre widgets et pages
- Rendu via widget renderer (`libs/ui/widget-renderer/`)

## Tests

```typescript
// Vitest pour les tests unitaires
describe('TicketFacade', () => {
  it('should return active tickets', () => {
    expect(facade.tickets()).toHaveLength(3);
  });
});

// Playwright pour E2E
test('should display ticket list', async ({ page }) => {
  await page.goto('/tenant/tickets');
  await expect(page.getByRole('list')).toBeVisible();
});
```

## Checklist avant tout composant

- [ ] `standalone: true` — jamais NgModule
- [ ] Control flow moderne (`@if`, `@for`) — jamais `*ngIf`, `*ngFor`
- [ ] Signaux pour le state local, NgRx pour le state global
- [ ] Façade entre composant et store
- [ ] 0 couleur hardcodée — CSS variables uniquement
- [ ] 0 string hardcodée — clés i18n snake_case
- [ ] Mobile-first (480px en base)
