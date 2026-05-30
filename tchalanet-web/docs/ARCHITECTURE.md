# tch-portal — Architecture

> **Statut** : Target architecture — migration en cours  
> **App** : `apps/tch-portal/` (Angular 20 / Nx)  
> **Référence détaillée** : [`docs/web/frontend-architecture-todo.md`](./web/frontend-architecture-todo.md)

---

## Structure cible

```
tchalanet-web/
├── apps/
│   └── tch-portal/         ← application Angular principale
└── libs/                   ← bibliothèques Nx (en cours de stabilisation)
    ├── api/                ← contrats backend/frontend, clients HTTP, interceptors
    ├── shared-auth/        ← OIDC/Keycloak, guards, secure storage
    ├── shared-i18n/        ← traduction, language switcher
    ├── shared-config/      ← environment, feature flags, settings
    ├── ui/                 ← design system (composants, thème, layout)
    ├── page-model/         ← moteur PageModel frontend
    ├── widgets/            ← registry + widgets dynamiques
    └── web/                ← routes, pages, containers par surface
```

---

## Surfaces applicatives

| Surface | Route prefix | Rôle |
|---|---|---|
| Public | `/public` | Résultats, vérification ticket, PageModel public |
| Cashier/POS | `/cashier` | Vente, paiement, session caisse |
| Private (tenant) | `/private` | Dashboard tenant, gestion |
| Tenant Admin | `/admin` | Configuration tenant |
| Platform Admin | `/platform` | Opérations plateforme |

---

## Convention composants

```
Route → Page → Container(s) → Component(s)
```

| Type | Suffixe | Règle |
|---|---|---|
| Page | `*.page.ts` | Routée, layout principal, peut injecter facade/store |
| Container | `*.container.ts` | Jamais routé, orchestre une sous-zone |
| Component | `*.component.ts` | Stateless/presentational, `input()`/`output()` |
| Widget | `*.widget.ts` | Rendu par PageModel, props uniquement |
| Shell | `*.shell.ts` | Structure globale d'une surface |

---

## Règles non négociables

- Toutes les routes pointent vers une `Page`
- Les composants UI ne font pas d'appel HTTP
- Les composants UI ne dépendent pas de NgRx/facades
- Les contrats backend/frontend vivent dans `libs/api/contracts`
- Les pages consomment des facades, pas directement des clients HTTP
- Pas de nouvelle lib sans frontière claire et stable

---

## Conventions

Voir [`docs/conventions/`](./conventions/README.md) :
- Naming, state management, Nx boundaries, feature playbook, placement guide.
