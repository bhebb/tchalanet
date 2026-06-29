# API

Common backend/Web transport boundary.

## Owns

- transverse HTTP contracts: `ApiResponse`, `ProblemDetail`, `TchPage`, `ActionItem`;
- response helpers: `unwrapApiResponse`, `hasApiNotices`;
- error mapping: `mapHttpErrorToProblemDetail`;
- HTTP interceptors: `correlationRequestInterceptor`, `problemDetailInterceptor`;
- typed backend client: `TchBackendClient`;
- context token: `SUPPRESS_SHELL_FEEDBACK`.
- query helpers: `appendQuery`, `toHttpParams`, `toQueryString`, `pageQuery`.

---

## TchBackendClient — norme d'utilisation

`TchBackendClient` est le seul accès autorisé à `HttpClient` pour les appels backend Tchalanet.

### Règle de layering

```
Page / Component
  → service client métier
    → TchBackendClient
      → Angular HttpClient
```

Les pages et composants ne doivent **pas** injecter `TchBackendClient` directement — seulement
via un service métier. Les services métier ne doivent **pas** injecter `HttpClient` directement.

### Usage de base

```ts
@Injectable({ providedIn: 'root' })
export class PublicResultsService {
  private readonly backend = inject(TchBackendClient);

  list(params: ResultsQuery): Observable<TchPage<ResultItem>> {
    return this.backend.get<TchPage<ResultItem>>('/public/results', { params });
  }
}
```

Le path logique `/public/results` est automatiquement préfixé en `/api/v1/public/results`.
Ne jamais inclure `/api/v1` dans les paths passés à `TchBackendClient`.

### Contrat paginé

`TchPage<T>` reflète le record backend `common.web.paging.TchPage` :

```ts
interface TchPage<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last?: boolean;
  hasNext?: boolean;
  hasPrevious?: boolean;
}
```

Ne pas utiliser le shape Spring `Page<T>` (`content`, `number`) pour les endpoints Tchalanet.

### Réponse complète (notices / serviceHealth)

```ts
listWithNotices(): Observable<ApiResponse<TchPage<ResultItem>>> {
  return this.backend.getApiResponse<TchPage<ResultItem>>('/public/results');
}
```

### Téléchargements bruts

```ts
exportPdf(id: string): Observable<Blob> {
  return this.backend.getBlob(`/tenant/reports/${id}/pdf`);
}
```

### Upload multipart

```ts
uploadAttachment(formData: FormData): Observable<AttachmentView> {
  return this.backend.postMultipart<AttachmentView>('/tenant/attachments', formData);
}
```

Ne jamais définir `Content-Type` manuellement pour FormData.

### Suppression du feedback shell

Utiliser quand la page gère ses erreurs localement :

```ts
this.backend.get('/public/results', { suppressShellFeedback: true });
```

### Override tenant (SUPER_ADMIN)

```ts
this.backend.get('/admin/identity/users', {
  asTenantAdmin: { tenantId, reason: 'Support action' },
});
```

### Retry

`TchBackendClient` ne retry pas globalement. Chaque service décide si un retry est safe :

```ts
// Safe (idempotent) :
return this.backend.get('/public/results').pipe(retry(2));

// Dangereux sans clé d'idempotence :
return this.backend.post('/tenant/tickets/sell', body); // ← pas de retry
```

### Ce que TchBackendClient ne fait pas

- Aucune logique métier.
- Aucun retry global.
- Aucun affichage UI.
- Aucun accès aux endpoints hors `/api/v1` (assets, i18n, Keycloak → `HttpClient` direct).
