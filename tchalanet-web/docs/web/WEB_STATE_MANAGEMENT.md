# Web State Management — Tchalanet

> Status: DRAFT v0.1  
> Scope: Angular Signals / SignalStore / NgRx

## 1. Modèle mental

```text
state global app        -> core
state API réutilisable  -> data-access
state d'écran           -> feature
state purement local    -> component signal
```

## 2. Niveaux de state

### 2.1 State local composant

Utiliser `signal()` pour :

```text
onglet sélectionné
modal ouverte/fermée
texte de recherche local
état hover/expanded
petit formulaire local
```

Exemple :

```ts
readonly selectedTab = signal<'open' | 'paid'>('open');
readonly searchText = signal('');
readonly isDialogOpen = signal(false);
```

### 2.2 State d'écran / feature

Va dans :

```text
features/<scope>/<feature>/<feature>.store.ts
```

Contient :

```text
filters
pagination
selected item
loading/error
view mode
dialog state
résultat de chargement propre à la page
```

Exemple :

```text
features/tenant/payouts/payouts.store.ts
features/platform/page-models/page-model-editor.store.ts
```

### 2.3 State API réutilisable

Va dans :

```text
data-access/<domain>/state
```

À utiliser seulement si plusieurs features ont besoin du même cache ou de la même ressource.

Exemples :

```text
data-access/tenant/state/tenant-config.store.ts
data-access/catalog/state/catalog-cache.store.ts
data-access/page-model/state/page-model-cache.store.ts
```

### 2.4 State global app

Va dans `core` :

```text
core/auth/auth-session.store.ts
core/config/runtime-config.store.ts
core/i18n/locale.store.ts
core/shell/navigation.store.ts
```

Contient :

```text
auth session
current user
permissions
runtime config
locale
theme courant
navigation globale
```

## 3. Signals vs SignalStore vs NgRx

### Signals natifs

Choisir pour state petit et local.

### SignalStore / store service local

Choisir pour écrans avec API, pagination, filtres, loading/error.

### NgRx Store global

Ne pas introduire par défaut.

À considérer seulement si :

```text
flows multi-pages complexes
besoin fort de time-travel/debug action log
beaucoup d'effets globaux
équipe large avec conventions NgRx déjà établies
```

Décision actuelle : commencer avec `Signals` + stores explicites. Ajouter NgRx seulement si besoin réel.

## 4. Règles de placement

| Besoin                           | Placement                                              |
| -------------------------------- | ------------------------------------------------------ |
| `isDialogOpen` dans un composant | composant avec `signal()`                              |
| filtres + pagination d'une page  | `features/.../*.store.ts`                              |
| session utilisateur              | `core/auth`                                            |
| permissions                      | `core/auth`                                            |
| locale                           | `core/i18n`                                            |
| tenant config utilisée partout   | `data-access/tenant/state` ou `core/shell` selon usage |
| cache d'un référentiel catalogue | `data-access/catalog/state`                            |
| état éditeur PageModel           | `features/platform/page-models`                        |

## 5. Anti-patterns

Interdit par défaut :

```text
shared/facades
shared/stores
BaseStore générique trop tôt
CrudStore générique trop tôt
un store global pour toutes les pages
duplications API dans les components
localStorage lu directement par les features
```

## 6. Exemple de store global auth

```ts
export interface AuthSession {
  userId: string;
  displayName: string;
  roles: string[];
  tenantCode?: string;
  permissions: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthSessionStore {
  private readonly _session = signal<AuthSession | null>(null);

  readonly session = this._session.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly permissions = computed(() => this._session()?.permissions ?? []);

  setSession(session: AuthSession): void {
    this._session.set(session);
  }

  clear(): void {
    this._session.set(null);
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }
}
```

## 7. Exemple de feature store

```ts
@Injectable()
export class PayoutsStore {
  private readonly api = inject(PayoutApiService);

  readonly page = signal(0);
  readonly size = signal(20);
  readonly status = signal<PayoutStatus | null>(null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<PayoutItem[]>([]);
  readonly total = signal(0);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(
        this.api.list({
          page: this.page(),
          size: this.size(),
          status: this.status(),
        }),
      );

      this.items.set(response.data.items);
      this.total.set(response.data.total);
    } catch {
      this.error.set('Unable to load payouts');
    } finally {
      this.loading.set(false);
    }
  }
}
```
