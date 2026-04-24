---
name: angular-nx
description: Use when writing or reviewing code in apps/tchalanet-web or libs/ — covers Angular 20 standalone components, Nx 21 monorepo, NgRx + signals, lazy routing, theming CSS variables, i18n @ngx-translate, Vitest unit tests, Playwright E2E, Nx generators, and affected commands.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Angular 20 + Nx 21 — Tchalanet Web

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

---

## Structure monorepo

```
apps/
└─ tchalanet-web/          ← app shell (routing + bootstrap UNIQUEMENT)
libs/
├─ shared/                 ← auth, API clients, state global, types, analytics
├─ ui/                     ← composants réutilisables, theming, widget-renderer
├─ web/                    ← pages, widgets, shell features (web-only)
└─ i18n/                   ← traductions fr/en/ht
```

**Règle absolue** : toute logique réutilisable dans `libs/`, jamais dans `apps/`.

---

## Composants — règles Angular 20

```typescript
// ✅ Standalone TOUJOURS — NgModule interdit
@Component({
  standalone: true,
  selector: 'tch-ticket-card',
  imports: [MatCardModule, MatButtonModule, TranslateModule, CurrencyPipe],
  templateUrl: './ticket-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush, // toujours OnPush
})
export class TicketCardComponent {
  ticket = input.required<TicketSummary>(); // input signal (Angular 17+)
  selected = output<TicketId>();            // output signal
}

// ❌ Jamais
@NgModule({ declarations: [...] }) // NgModule interdit
*ngIf="condition"                  // ancienne syntaxe
*ngFor="let item of items"         // ancienne syntaxe
```

---

## Control flow moderne — OBLIGATOIRE

```html
@if (isLoading()) {
<mat-spinner />
} @else if (tickets().length === 0) {
<p>{{ 'ticket.empty_state' | translate }}</p>
} @else { @for (ticket of tickets(); track ticket.id) {
<tch-ticket-card [ticket]="ticket" />
} } @defer (on viewport) {
<tch-heavy-chart />
}
```

---

## Signaux — patterns courants

```typescript
// State local → signals
count = signal(0);
doubled = computed(() => this.count() * 2);

// Depuis le store → toSignal
tickets = toSignal(this.store.select(selectActiveTickets), { initialValue: [] });

// input/output signals (Angular 17+)
value = input<string>('');
valueChange = output<string>();
```

---

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
// Actions
export const TicketActions = createActionGroup({
  source: 'Ticket',
  events: {
    'Load Active': emptyProps(),
    'Load Active Success': props<{ tickets: TicketSummary[] }>(),
    'Load Active Failure': props<{ error: string }>(),
  },
});

// Facade — les composants n'accèdent JAMAIS au store directement
@Injectable({ providedIn: 'root' })
export class TicketFacade {
  tickets = toSignal(this.store.select(selectActiveTickets), { initialValue: [] });
  load() {
    this.store.dispatch(TicketActions.loadActive());
  }
}
```

---

## Theming et styles

```scss
// ✅ Jamais de couleur hardcodée — CSS variables uniquement
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

---

## i18n — @ngx-translate

```typescript
// Clés snake_case avec namespaces — 3 locales obligatoires : fr, en, ht
// libs/i18n/src/assets/fr.json
{
  "ticket": {
    "list_title": "Mes tickets",
    "status_active": "Actif",
    "action_buy": "Acheter"
  }
}
```

```html
<!-- ✅ Dans le template -->
{{ 'ticket.list_title' | translate }}

<!-- ❌ Jamais de string hardcodée -->
<h1>Mes tickets</h1>
```

---

## Widgets vs Pages

```
libs/web/src/lib/
├─ pages/      ← routing, layout, orchestration (thin)
└─ widgets/    ← blocs UI autonomes avec leur propre state local
```

- Pages → orchestrent les widgets, pas de logique UI directe
- Widgets → autonomes, réutilisables, pas de logique dupliquée

---

## Routing — lazy loading

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: 'tickets',
    loadChildren: () => import('@tchalanet/web/tickets').then(m => m.TICKET_ROUTES),
  },
  {
    path: 'draws',
    loadChildren: () => import('@tchalanet/web/draws').then(m => m.DRAW_ROUTES),
  },
];
```

---

## Barrel exports — libs/

```typescript
// libs/ui/src/index.ts — toujours exporter depuis index.ts
export { TicketCardComponent } from './lib/ticket-card/ticket-card.component';

// Import dans d'autres libs/apps
import { TicketCardComponent } from '@tchalanet/ui'; // path mapping
```

---

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

---

## Nx — commandes essentielles

```bash
# Générer
nx g @angular/core:component my-comp --project=ui --standalone
nx g @nx/angular:library my-lib --directory=libs/my-lib --standalone

# Dev
nx serve tchalanet-web
nx test tchalanet-web
nx e2e tchalanet-web-e2e
nx build tchalanet-web --configuration=production

# Optimisation CI — seulement les projets touchés
nx affected -t test
nx affected -t build
nx affected -t lint

# Visualiser les dépendances
nx graph

# Reset cache si problème
nx reset
```

---

## Checklist composant Angular

- [ ] `standalone: true` — zéro NgModule
- [ ] `ChangeDetectionStrategy.OnPush`
- [ ] Control flow moderne (`@if`, `@for`, `@defer`)
- [ ] Signaux pour state local, NgRx + Facade pour state global
- [ ] `input()` / `output()` signals (pas `@Input()`/`@Output()`)
- [ ] 0 couleur hardcodée — CSS variables uniquement
- [ ] 0 string hardcodée — clés i18n `snake_case`
- [ ] Mobile-first (480px en base)
- [ ] Exporté depuis le `index.ts` de sa lib
