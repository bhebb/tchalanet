import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApiResponse, unwrapApiResponse } from '@tch/api';
import { map, Observable } from 'rxjs';

import { PageRuntimeResponse } from '../runtime/pagemodel.types';

/**
 * Typed client for the backend PageModel runtime endpoints.
 *
 * Each endpoint wraps its payload in `ApiResponse<T>`; we unwrap to the bare response so callers
 * consume the resolved runtime contract directly. Mirrors the pattern in `SettingsApi`.
 */
@Injectable({ providedIn: 'root' })
export class PageModelApi {
  private readonly http = inject(HttpClient);

  /** Anonymous public page. The backend resolves the single public PageModel. */
  getPublicPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.http
      .get<ApiResponse<PageRuntimeResponse>>('/api/v1/public/page', { params: langParams(lang) })
      .pipe(map(unwrapApiResponse));
  }

  /** Public managers page (gestionnaires). */
  getPublicManagersPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.http
      .get<ApiResponse<PageRuntimeResponse>>('/api/v1/public/managers', {
        params: langParams(lang),
      })
      .pipe(map(unwrapApiResponse));
  }

  /** SUPER_ADMIN dashboard, resolved server-side by role. */
  getPlatformPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.http
      .get<ApiResponse<PageRuntimeResponse>>('/api/v1/platform/dashboard', {
        params: langParams(lang),
      })
      .pipe(map(unwrapApiResponse));
  }

  /** TENANT_ADMIN dashboard, resolved server-side from request context. */
  getTenantPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.http
      .get<ApiResponse<PageRuntimeResponse>>('/api/v1/tenant/dashboard', {
        params: langParams(lang),
      })
      .pipe(map(unwrapApiResponse));
  }
}

function langParams(lang?: string): HttpParams | undefined {
  return lang ? new HttpParams().set('lang', lang) : undefined;
}
