# Design — backend-client-shell-feedback

## Architecture cible

```
Page / Component
  → service client métier (ex. PublicResultsService)
    → TchBackendClient
      → Angular HttpClient
        ← ApiFeedbackInterceptor (observe réponses → ShellFeedbackStore)
          ← correlationRequestInterceptor (existant, libs/api)
          ← problemDetailInterceptor (existant, libs/api)
```

---

## Structure fichiers

```
src/app/shared/api/
  tch-backend-client.ts           service principal
  api-feedback.interceptor.ts     interceptor → ShellFeedbackStore
  api-feedback-context.ts         HttpContextToken suppressShellFeedback

src/app/shared/feedback/
  shell-feedback.model.ts         ShellFeedbackItem, ShellFeedbackSeverity, AddShellFeedbackInput
  shell-feedback.store.ts         ShellFeedbackStore (signal store)
  shell-feedback-outlet.component.ts   conteneur shell
  shell-feedback-banner.component.ts   bannière individuelle
  copy-error-details.ts           utilitaire copie support-safe
```

`libs/api` reste inchangé. `TchBackendClient` importe depuis `@tch/api` :
- `TCH_API_BASE`
- `ApiResponse<T>`
- `ProblemDetail`
- `unwrapApiResponse`

---

## TchBackendClient

### Nom canonique

`TchBackendClient` — ne pas utiliser `TchHttpClient`, `ApiClient`, `ApiService`.

### Base path

`TchBackendClient` injecte `TCH_API_BASE` (défaut `/api/v1`).  
Les services métier passent uniquement le path logique ; `TchBackendClient` préfixe.  
Exception : les URLs absolues (`http://`, `https://`) sont passées telles quelles.

### Signature

```ts
// Unwrapped (défaut)
get<T>(path: string, options?: TchRequestOptions): Observable<T>
post<TResponse, TBody = unknown>(path: string, body: TBody, options?: TchRequestOptions): Observable<TResponse>
put<TResponse, TBody = unknown>(path: string, body: TBody, options?: TchRequestOptions): Observable<TResponse>
patch<TResponse, TBody = unknown>(path: string, body: TBody, options?: TchRequestOptions): Observable<TResponse>
delete<TResponse>(path: string, options?: TchRequestOptions): Observable<TResponse>

// Réponse complète (notices/services nécessaires)
getApiResponse<T>(path: string, options?: TchRequestOptions): Observable<ApiResponse<T>>
postApiResponse<TResponse, TBody = unknown>(path: string, body: TBody, options?: TchRequestOptions): Observable<ApiResponse<TResponse>>

// Raw downloads (non unwrappés)
getBlob(path: string, options?: TchRequestOptions): Observable<Blob>
getBlobResponse(path: string, options?: TchRequestOptions): Observable<HttpResponse<Blob>>
getArrayBuffer(path: string, options?: TchRequestOptions): Observable<ArrayBuffer>
getText(path: string, options?: TchRequestOptions): Observable<string>

// Upload multipart
postMultipart<TResponse>(path: string, formData: FormData, options?: TchRequestOptions): Observable<TResponse>
putMultipart<TResponse>(path: string, formData: FormData, options?: TchRequestOptions): Observable<TResponse>
postMultipartBlob(path: string, formData: FormData, options?: TchRequestOptions): Observable<Blob>
```

### TchRequestOptions

```ts
interface TchRequestOptions {
  params?: HttpParams | Record<string, string | string[]>;
  headers?: HttpHeaders | Record<string, string>;
  suppressShellFeedback?: boolean;
  asTenantAdmin?: {
    tenantId: string;
    reason: string;
  };
}
```

### Override tenant (SUPER_ADMIN)

L'option `asTenantAdmin` produit les headers :

```
X-Tch-Tenant-Override: <tenantId>
X-Tch-Act-As: TENANT_ADMIN
X-Tch-Override-Reason: <reason>
```

- Jamais envoyés implicitement.
- Le backend valide que le caller est SUPER_ADMIN et audite l'override.

### Retry

Aucun retry global. Chaque service métier décide si un retry est safe pour son endpoint.

---

## ApiFeedbackInterceptor

- Observe **toutes** les réponses HTTP passant par `HttpClient`.
- Filtre les appels portant `SUPPRESS_SHELL_FEEDBACK` dans le contexte.
- Sur succès `ApiResponse<T>` : envoie `notices` et services `DOWN`/`DEGRADED` vers `ShellFeedbackStore`.
- Sur erreur `ProblemDetail` : envoie un feedback `error` vers `ShellFeedbackStore`.
- **Ne jamais avaler les erreurs** : elles continuent vers les services/pages.

### Sévérité des notices

| Source | Sévérité notice/service | Sévérité shell |
|---|---|---|
| `ApiNotice.severity = info` | info | info |
| `ApiNotice.severity = warning` | warning | warn |
| `ServiceStatus.healthy = false` | — | warn ou error |
| `ProblemDetail 4xx client` | — | warn |
| `ProblemDetail 5xx server` | — | error |

---

## api-feedback-context.ts

```ts
export const SUPPRESS_SHELL_FEEDBACK = new HttpContextToken<boolean>(() => false);
```

`TchBackendClient` injecte ce token quand `suppressShellFeedback: true` est passé dans les options.

---

## ShellFeedbackStore

Signal store mince.

```ts
state: {
  items: ShellFeedbackItem[];
}
computed: {
  hasItems: boolean;
}
methods: {
  add(input: AddShellFeedbackInput): void;
  dismiss(id: string): void;
  clear(): void;
}
```

- Limite le nombre de messages visibles (ex. max 5 simultanés).
- Les messages sont dismissibles par défaut.
- N'appelle pas HTTP, ne navigue pas, ne connaît pas les pages.

---

## ShellFeedbackItem

```ts
export type ShellFeedbackSeverity = 'info' | 'warn' | 'error';

export interface ShellFeedbackItem {
  readonly id: string;                  // généré en interne
  readonly severity: ShellFeedbackSeverity;
  readonly title: string;
  readonly message: string;
  readonly source?: string;
  readonly traceId?: string;
  readonly status?: number;
  readonly copyText?: string;           // texte support-safe pour copie
  readonly dismissible: boolean;
}
```

---

## ShellFeedbackOutletComponent

- Placé dans les shells : `<tch-shell-feedback-outlet [verbosity]="'minimal'" />`
- Consomme `ShellFeedbackStore` via signal.
- Rend `<tch-shell-feedback-banner>` pour chaque item.
- Le `verbosity` est un `Input` requis ; le shell déclare le niveau, pas le store.

### Niveaux de verbosité

```ts
export type ShellFeedbackVerbosity = 'minimal' | 'standard' | 'verbose';
```

| Niveau | Shell cible | Titre + message | Trace ID | Status | Source/service | Bouton copier |
|---|---|---|---|---|---|---|
| `minimal` | Public | Oui | Non | Non | Non | Non |
| `standard` | Tenant admin | Oui | Oui (si dispo) | Non | Non | Oui |
| `verbose` | Platform / SUPER_ADMIN | Oui | Oui (si dispo) | Oui (si dispo) | Oui (si dispo) | Oui |

### Règles

- `minimal` : adapté à un utilisateur public, aucune donnée technique exposée.
- `standard` : trace id visible pour le support interne, copie support-safe disponible.
- `verbose` : status HTTP, source/service, trace id, copie — réservé platform / SUPER_ADMIN.
- `ShellFeedbackBannerComponent` reçoit `item` + `verbosity` en `Input` et conditionne
  l'affichage de chaque champ.
- Le bouton copier n'est rendu que pour `standard` et `verbose`.

---

## copy-error-details.ts

Format support-safe copié :

```
Erreur Tchalanet
Statut: <status>
Titre: <title>
Message: <message>
Trace ID: <traceId>
Chemin: <path>
Date: <ISO date>
```

Règles : pas de stacktrace, pas de JWT, pas de données personnelles, pas de body sensible.

---

## Exports publics

Depuis `shared/api` :

```
TchBackendClient
TchRequestOptions
SUPPRESS_SHELL_FEEDBACK
```

Depuis `shared/feedback` :

```
ShellFeedbackSeverity
ShellFeedbackItem
AddShellFeedbackInput
ShellFeedbackStore
ShellFeedbackOutletComponent
ShellFeedbackBannerComponent
```

Ne pas exporter : helpers internes, fonctions de normalisation privées, générateurs d'id.

---

## Dépendances vers libs/api

`TchBackendClient` et `ApiFeedbackInterceptor` importent depuis `@tch/api` :

```
ApiResponse<T>
ProblemDetail
TCH_API_BASE
unwrapApiResponse
```

Aucune duplication de ces types dans `shared/api/`.

---

## Considérations

- `libs/api` exporte déjà `TCH_API_BASE` avec default `/api/v1`. `TchBackendClient` l'injecte
  sans redéfinir de token.
- Les services `PageModelApi` et `SettingsApi` dans `libs/` migrent dans la slice 5 en injectant
  `TchBackendClient`. Leurs tests unitaires mockent `TchBackendClient` à la place de `HttpClient`.
- Les appels hors backend Tchalanet (assets, i18n, Keycloak OIDC) ne passent pas par
  `TchBackendClient` et conservent `HttpClient` direct.
