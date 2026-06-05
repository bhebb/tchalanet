import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  ApiResponse,
  DashboardPageModelResponse,
  PublicPageModelResponse,
} from '../../shared/types';
import { unwrapApiResponse } from '../http';

/**
 * Typed client for the backend PageModel runtime endpoints.
 *
 * Each endpoint wraps its payload in `ApiResponse<T>`; we unwrap to the bare response so callers
 * consume the real `PageModelDoc` contract directly. Mirrors the pattern in `SettingsApi`.
 */
@Injectable({ providedIn: 'root' })
export class PageModelApi {
  private readonly http = inject(HttpClient);

  /** Anonymous public page (e.g. `public.home`). No authenticated session required. */
  getPublicPage(logicalId: string, lang?: string): Observable<PublicPageModelResponse> {
    return this.http
      .get<ApiResponse<PublicPageModelResponse>>(
        `/api/v1/public/page-models/${encodeURIComponent(logicalId)}`,
        { params: langParams(lang) },
      )
      .pipe(map(unwrapApiResponse));
  }

  /** SUPER_ADMIN dashboard, resolved server-side by role. */
  getPlatformPage(lang?: string): Observable<DashboardPageModelResponse> {
    return this.http
      .get<ApiResponse<DashboardPageModelResponse>>('/api/v1/platform/page-models', {
        params: langParams(lang),
      })
      .pipe(map(unwrapApiResponse));
  }

  /** TENANT_ADMIN dashboard, resolved server-side from request context. */
  getTenantPage(lang?: string): Observable<DashboardPageModelResponse> {
    return this.http
      .get<ApiResponse<DashboardPageModelResponse>>('/api/v1/tenant/page-models', {
        params: langParams(lang),
      })
      .pipe(map(unwrapApiResponse));
  }
}

function langParams(lang?: string): HttpParams | undefined {
  return lang ? new HttpParams().set('lang', lang) : undefined;
}
