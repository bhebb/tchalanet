---
name: angular-nx
description: >
  Déclencher pour tout code Angular 20, Nx 21, composants standalone, signals,
  NgRx, routing, lazy loading, Nx generators, affected commands, ou configuration
  monorepo. Indispensable si la tâche concerne : libs/, apps/tchalanet-web,
  nx.json, project.json, barrel exports, ou optimisation du build Nx.
---

# Angular 20 + Nx 21 — Tchalanet

> Angular 20.2.4 · Nx 21.4.1 · Vite 6 · pnpm 10.19.0

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
  selected = output<TicketId>(); // output signal
}
```

## Signaux — patterns courants

```typescript
// State local → signals
count = signal(0);
doubled = computed(() => this.count() * 2);

// Depuis le store → toSignal
tickets = toSignal(this.store.select(selectActiveTickets), { initialValue: [] });

// Effect pour side-effects
effect(() => {
  if (this.tickets().length === 0) this.loadTickets();
});

// input/output signals (Angular 17+)
value = input<string>('');
valueChange = output<string>();
```

## Control flow moderne — OBLIGATOIRE

```html
<!-- ✅ Nouveau control flow -->
@if (isLoading()) {
<mat-spinner />
} @else if (tickets().length === 0) {
<p>{{ 'ticket.empty_state' | translate }}</p>
} @else { @for (ticket of tickets(); track ticket.id) {
<tch-ticket-card [ticket]="ticket" />
} } @defer (on viewport) {
<tch-heavy-chart />
}

<!-- ❌ Interdit -->
<div *ngIf="...">
  <div *ngFor="let t of tickets"></div>
</div>
```

## NgRx — structure par feature

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

// Selectors
export const selectActiveTickets = createSelector(selectTicketState, state =>
  state.tickets.filter(t => t.status === 'PLACED'),
);

// Facade — les composants n'accèdent JAMAIS au store directement
@Injectable({ providedIn: 'root' })
export class TicketFacade {
  tickets = toSignal(this.store.select(selectActiveTickets), { initialValue: [] });
  load() {
    this.store.dispatch(TicketActions.loadActive());
  }
}
```

## Nx — commandes essentielles

```bash
# Générer
nx g @angular/core:component my-comp --project=ui --standalone
nx g @nx/angular:library my-lib --directory=libs/my-lib --standalone

# Dev
nx serve tchalanet-web
nx test tchalanet-web
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

## Nx — project.json essentiel

```json
{
  "name": "ui",
  "targets": {
    "build": { "executor": "@nx/vite:build" },
    "test": { "executor": "@nx/vite:test" },
    "lint": { "executor": "@nx/eslint:lint" }
  },
  "tags": ["scope:ui", "type:lib"]
}
```

## Barrel exports — libs/

```typescript
// libs/ui/src/index.ts — toujours exporter depuis index.ts
export { TicketCardComponent } from './lib/ticket-card/ticket-card.component';
export { ButtonComponent } from './lib/button/button.component';

// Import dans d'autres libs/apps
import { TicketCardComponent } from '@tchalanet/ui'; // path mapping
```

## Routing — lazy loading

```typescript
// app.routes.ts — lazy loading par feature
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

## Checklist composant Angular

- [ ] `standalone: true` — zéro NgModule
- [ ] `ChangeDetectionStrategy.OnPush`
- [ ] Control flow moderne (`@if`, `@for`, `@defer`)
- [ ] Signaux pour state local, NgRx + Facade pour state global
- [ ] `input()` / `output()` signals (pas `@Input()`/`@Output()`)
- [ ] 0 couleur hardcodée — CSS variables uniquement
- [ ] 0 string hardcodée — clés i18n `snake_case`
- [ ] Exporté depuis le `index.ts` de sa lib
