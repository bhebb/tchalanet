# Web Feature Playbook — Tchalanet

> Status: DRAFT v0.1

## 1. Question de départ

Avant d'ajouter une feature, décider :

```text
Est-ce une route/page/flow utilisateur ? -> features
Est-ce un appel backend/contrat API ? -> data-access
Est-ce un composant visuel pur ? -> ui
Est-ce global technique ? -> core
Est-ce une fonction générique ? -> shared
```

## 2. Emplacement d'une feature

Par défaut :

```text
apps/tch-web/src/app/features/<scope>/<feature>
```

Exemples :

```text
features/public/home
features/public/ticket-verify
features/tenant/payouts
features/admin/outlets
features/platform/page-models
```

## 3. Structure petite feature

```text
features/tenant/payouts/
  payouts.routes.ts
  payouts.page.ts
  payouts.store.ts
  payout-list.component.ts
  payout-detail-panel.component.ts
```

Règle : ne pas créer de sous-dossiers automatiquement.

## 4. Structure grosse feature

Quand il y a au moins 3 fichiers d'un même rôle :

```text
features/tenant/payouts/
  payouts.routes.ts

  pages/
    payouts.page.ts
    payout-details.page.ts

  components/
    payout-list.component.ts
    payout-detail-panel.component.ts
    payout-status-badge.component.ts

  state/
    payouts.store.ts
    payout-details.store.ts

  mappers/
    payout-page.mapper.ts
```

## 5. Ajouter une feature étape par étape

### Étape 1 — Créer la route

```ts
export const payoutRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./payouts.page').then(m => m.PayoutsPage),
  },
];
```

### Étape 2 — Créer la page

La page compose l'écran. Elle ne doit pas appeler `HttpClient` directement.

### Étape 3 — Créer le store d'écran si nécessaire

Le store d'écran garde : filters, loading, error, selected item, pagination.

### Étape 4 — Utiliser `data-access`

Si l'API n'existe pas encore :

```text
libs/data-access/<domain>/api
libs/data-access/<domain>/model
```

### Étape 5 — Utiliser `ui`

Les composants visuels génériques vont dans `ui` seulement s'ils sont réutilisables et métier-agnostiques.

## 6. Ce qui est interdit dans une feature

```text
business rules critiques recodées côté frontend
HttpClient direct si data-access existe
lecture directe localStorage/sessionStorage
construction manuelle de token/auth headers
composants design-system mélangés avec logique API
```

## 7. Definition of Done

- [ ] Feature sous le bon scope : public / tenant / admin / platform.
- [ ] Route lazy si applicable.
- [ ] Pas d'appel `HttpClient` direct depuis le composant.
- [ ] API dans `data-access`.
- [ ] State placé au bon niveau.
- [ ] Composants UI purs extraits seulement si réutilisables.
- [ ] Pas de logique métier critique côté frontend.
- [ ] Noms explicites et searchable.
