import {
  HttpClient,
  HttpContext,
  HttpHeaders,
  HttpParams,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, Injector, ResourceRef, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiResponse, TchBackendPage, TchPage } from '../contracts/api.types';
import { TCH_API_BASE, TCH_API_BASE_RESOLVER } from '../http/api-base';
import { SUPPRESS_SHELL_FEEDBACK } from '../http/api-feedback-context';
import { unwrapApiResponse } from '../http/api-response';

export interface TchRequestOptions {
  readonly params?: HttpParams | Record<string, string | ReadonlyArray<string>>;
  readonly headers?: HttpHeaders | Record<string, string>;
  readonly suppressShellFeedback?: boolean;
  readonly asTenantAdmin?: {
    readonly tenantId: string;
    readonly reason: string;
  };
}

type ResolvedOptions = {
  params?: HttpParams;
  headers?: HttpHeaders;
  context?: HttpContext;
};

/** Requête déclarative pour les factories de resources (`getResource`/`getPageResource`). */
export interface TchResourceRequest {
  readonly path: string;
  readonly options?: TchRequestOptions;
}

@Injectable({ providedIn: 'root' })
export class TchBackendClient {
  private readonly http = inject(HttpClient);
  private readonly fallbackBase = inject(TCH_API_BASE);
  private readonly resolveBase = inject(TCH_API_BASE_RESOLVER, { optional: true });
  private readonly injector = inject(Injector);

  private url(path: string): string {
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${this.resolveBase?.() || this.fallbackBase}${path}`;
  }

  private resolve(options?: TchRequestOptions): ResolvedOptions {
    const result: ResolvedOptions = {};

    if (options?.params) {
      result.params =
        options.params instanceof HttpParams
          ? options.params
          : new HttpParams({
              fromObject: options.params as Record<string, string | readonly string[]>,
            });
    }

    if (options?.asTenantAdmin || options?.headers) {
      let headers =
        options.headers instanceof HttpHeaders
          ? options.headers
          : new HttpHeaders(options?.headers ?? {});

      if (options?.asTenantAdmin) {
        headers = headers
          .set('X-Tch-Tenant-Override', options.asTenantAdmin.tenantId)
          .set('X-Tch-Act-As', 'TENANT_ADMIN')
          .set('X-Tch-Override-Reason', options.asTenantAdmin.reason);
      }

      result.headers = headers;
    }

    if (options?.suppressShellFeedback) {
      result.context = new HttpContext().set(SUPPRESS_SHELL_FEEDBACK, true);
    }

    return result;
  }

  get<T>(path: string, options?: TchRequestOptions): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.url(path), this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  getPage<T>(path: string, options?: TchRequestOptions): Observable<TchPage<T>> {
    const fallback = this.pageFallback(options);

    return this.http
      .get<ApiResponse<TchBackendPage<T>>>(this.url(path), this.resolve(options))
      .pipe(
        map(unwrapApiResponse),
        map(page => this.toPage(page, fallback.page, fallback.size)),
      );
  }

  post<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .post<ApiResponse<TResponse>>(this.url(path), body, this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  put<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .put<ApiResponse<TResponse>>(this.url(path), body, this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  patch<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .patch<ApiResponse<TResponse>>(this.url(path), body, this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  delete<TResponse>(path: string, options?: TchRequestOptions): Observable<TResponse> {
    return this.http
      .delete<ApiResponse<TResponse>>(this.url(path), this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  deleteWithBody<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .request<ApiResponse<TResponse>>('DELETE', this.url(path), {
        ...this.resolve(options),
        body,
      })
      .pipe(map(unwrapApiResponse));
  }

  /**
   * Resource de lecture GET — seul point de création de resources backend
   * (les features n'instancient jamais `rxResource`/`httpResource` elles-mêmes).
   *
   * `request()` est réactif : tout signal lu dedans redéclenche le chargement en
   * annulant la requête précédente. Renvoyer `undefined` laisse le resource `idle`
   * (chargement lazy — onglet non visité, paramètre manquant).
   *
   * `project` mappe le DTO backend (`TRaw`) vers la vue métier (`T`) au niveau du
   * client : le data-access expose ainsi un resource déjà projeté, sans mapping
   * dans la page.
   */
  getResource<T, TRaw = T>(
    request: () => TchResourceRequest | undefined,
    project?: (raw: TRaw) => T,
  ): ResourceRef<T | undefined> {
    return rxResource<T, TchResourceRequest | undefined>({
      injector: this.injector,
      params: () => request(),
      // le runtime ne déclenche pas le stream quand params est undefined (lazy)
      stream: ({ params }) => {
        const req = params as TchResourceRequest;
        const raw$ = this.get<TRaw>(req.path, req.options);
        return (project ? raw$.pipe(map(project)) : raw$) as unknown as Observable<T>;
      },
    });
  }

  /**
   * Comme {@link getResource}, pour les listes paginées `TchPage<T>`.
   * `project` mappe chaque item du DTO backend (`TRaw`) vers la vue métier (`T`).
   */
  getPageResource<T, TRaw = T>(
    request: () => TchResourceRequest | undefined,
    project?: (raw: TRaw) => T,
  ): ResourceRef<TchPage<T> | undefined> {
    return rxResource<TchPage<T>, TchResourceRequest | undefined>({
      injector: this.injector,
      params: () => request(),
      stream: ({ params }) => {
        const req = params as TchResourceRequest;
        const page$ = this.getPage<TRaw>(req.path, req.options);
        return (
          project
            ? page$.pipe(map(page => ({ ...page, items: page.items.map(project) })))
            : page$
        ) as unknown as Observable<TchPage<T>>;
      },
    });
  }

  getApiResponse<T>(path: string, options?: TchRequestOptions): Observable<ApiResponse<T>> {
    return this.http.get<ApiResponse<T>>(this.url(path), this.resolve(options));
  }

  postApiResponse<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<ApiResponse<TResponse>> {
    return this.http.post<ApiResponse<TResponse>>(this.url(path), body, this.resolve(options));
  }

  getBlob(path: string, options?: TchRequestOptions): Observable<Blob> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), { params, headers, context, responseType: 'blob' });
  }

  getBlobResponse(path: string, options?: TchRequestOptions): Observable<HttpResponse<Blob>> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), {
      params,
      headers,
      context,
      responseType: 'blob',
      observe: 'response',
    });
  }

  postBlob<TBody = unknown>(
    path: string,
    body: TBody,
    options?: TchRequestOptions,
  ): Observable<Blob> {
    const { params, headers, context } = this.resolve(options);
    return this.http.post(this.url(path), body, {
      params,
      headers,
      context,
      responseType: 'blob',
    });
  }

  getArrayBuffer(path: string, options?: TchRequestOptions): Observable<ArrayBuffer> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), { params, headers, context, responseType: 'arraybuffer' });
  }

  getText(path: string, options?: TchRequestOptions): Observable<string> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), { params, headers, context, responseType: 'text' });
  }

  postMultipart<TResponse>(
    path: string,
    formData: FormData,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .post<ApiResponse<TResponse>>(this.url(path), formData, this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  putMultipart<TResponse>(
    path: string,
    formData: FormData,
    options?: TchRequestOptions,
  ): Observable<TResponse> {
    return this.http
      .put<ApiResponse<TResponse>>(this.url(path), formData, this.resolve(options))
      .pipe(map(unwrapApiResponse));
  }

  postMultipartBlob(
    path: string,
    formData: FormData,
    options?: TchRequestOptions,
  ): Observable<Blob> {
    const { params, headers, context } = this.resolve(options);
    return this.http.post(this.url(path), formData, {
      params,
      headers,
      context,
      responseType: 'blob',
    });
  }

  private toPage<T>(page: TchBackendPage<T>, fallbackPage = 0, fallbackSize = 20): TchPage<T> {
    const items = [...(page.items ?? page.content ?? [])];
    const size = page.size ?? fallbackSize;
    const totalElements = page.totalElements ?? page.total ?? items.length;
    const pageNumber = page.page ?? page.number ?? fallbackPage;
    const totalPages = page.totalPages ?? Math.max(1, Math.ceil(totalElements / Math.max(1, size)));

    return {
      items,
      totalElements,
      totalPages,
      page: pageNumber,
      size,
      last: page.last ?? pageNumber + 1 >= totalPages,
      hasNext: page.hasNext ?? pageNumber + 1 < totalPages,
      hasPrevious: page.hasPrevious ?? pageNumber > 0,
    };
  }

  private pageFallback(options?: TchRequestOptions): {
    readonly page: number;
    readonly size: number;
  } {
    return {
      page: this.numberParam(options, 'page') ?? 0,
      size: this.numberParam(options, 'size') ?? 20,
    };
  }

  private numberParam(
    options: TchRequestOptions | undefined,
    key: 'page' | 'size',
  ): number | undefined {
    const params = options?.params;
    if (!params) return undefined;

    const rawValue = params instanceof HttpParams ? params.get(key) : params[key];
    const value = Array.isArray(rawValue) ? rawValue[0] : rawValue;
    if (value === undefined || value === null || value === '') return undefined;

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
}
