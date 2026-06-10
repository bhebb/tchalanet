import {
  HttpClient,
  HttpContext,
  HttpHeaders,
  HttpParams,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiResponse } from '../contracts/api.types';
import { TCH_API_BASE } from './api-base';
import { SUPPRESS_SHELL_FEEDBACK } from './api-feedback-context';
import { unwrapApiResponse } from './api-response';

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

@Injectable({ providedIn: 'root' })
export class TchBackendClient {
  private readonly http = inject(HttpClient);
  private readonly base = inject(TCH_API_BASE);

  private url(path: string): string {
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${this.base}${path}`;
  }

  private resolve(options?: TchRequestOptions): ResolvedOptions {
    const result: ResolvedOptions = {};

    if (options?.params) {
      result.params =
        options.params instanceof HttpParams
          ? options.params
          : new HttpParams({ fromObject: options.params as Record<string, string | readonly string[]> });
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

  // ── Unwrapped (default) ────────────────────────────────────────────────────

  get<T>(path: string, options?: TchRequestOptions): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.url(path), this.resolve(options))
      .pipe(map(unwrapApiResponse));
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

  // ── Full ApiResponse (notices / serviceHealth needed) ─────────────────────

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

  // ── Raw downloads ──────────────────────────────────────────────────────────

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

  getArrayBuffer(path: string, options?: TchRequestOptions): Observable<ArrayBuffer> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), { params, headers, context, responseType: 'arraybuffer' });
  }

  getText(path: string, options?: TchRequestOptions): Observable<string> {
    const { params, headers, context } = this.resolve(options);
    return this.http.get(this.url(path), { params, headers, context, responseType: 'text' });
  }

  // ── Multipart uploads ──────────────────────────────────────────────────────

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
}
