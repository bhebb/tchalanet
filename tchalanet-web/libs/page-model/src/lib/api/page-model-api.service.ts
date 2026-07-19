import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ApiNotice, ApiResponse, TchBackendClient } from '@tch/api';
import { TCH_CONFIG_ASSETS } from '@tch/shared-assets';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { PageRuntimeResponse, WidgetDynamicError } from '../runtime/pagemodel.types';

@Injectable({ providedIn: 'root' })
export class PageModelApi {
  private readonly backend = inject(TchBackendClient);
  private readonly http = inject(HttpClient);

  getPublicPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/public/page', {
      params: langParams(lang),
      feedback: { owner: 'feature', mode: 'local', target: 'page-model' },
    }).pipe(map(withSectionNotices));
  }

  getPublicManagersPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/public/managers', {
      params: langParams(lang),
    }).pipe(map(withSectionNotices));
  }

  getPlatformPage(
    logicalId = 'private.dashboard.superadmin',
    lang?: string,
  ): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/platform/dashboard', {
      params: logicalIdParams(logicalId, lang),
    }).pipe(map(withSectionNotices));
  }

  getTenantPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/tenant/dashboard', {
      params: langParams(lang),
    }).pipe(map(withSectionNotices));
  }

  getCashierHomePage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/tenant/cashier/home', {
      params: langParams(lang),
    }).pipe(map(withSectionNotices));
  }

  getPrivateFallbackPage(): Observable<PageRuntimeResponse> {
    return this.http.get<PageRuntimeResponse>(TCH_CONFIG_ASSETS.pagePrivateFallback);
  }
}

function withSectionNotices(response: ApiResponse<PageRuntimeResponse>): PageRuntimeResponse {
  assertPageRuntimeResponse(response.data);

  const existingErrors = response.data.dynamic.errors.map(sanitizeWidgetError);
  const existingKeys = new Set(existingErrors.map(error => widgetErrorKey(error.widgetId, error.code)));
  const widgetErrors = (response.notices ?? [])
    .map(notice => widgetErrorFromNotice(notice, response.data, response.trace))
    .filter((error): error is WidgetDynamicError => error !== null)
    .filter(error => !existingKeys.has(widgetErrorKey(error.widgetId, error.code)));

  if (!widgetErrors.length && !existingErrors.length) return response.data;

  return {
    ...response.data,
    dynamic: {
      ...response.data.dynamic,
      errors: [
        ...existingErrors,
        ...widgetErrors,
      ],
    },
  };
}

function assertPageRuntimeResponse(data: PageRuntimeResponse | null | undefined): asserts data is PageRuntimeResponse {
  if (!data?.content?.layout || !data.dynamic?.widgets || !data.dynamic.errors) {
    throw new Error('Invalid PageRuntimeResponse');
  }
}

function widgetErrorKey(widgetId: string, code: string | undefined): string {
  return `${widgetId}|${code ?? ''}`;
}

function widgetErrorFromNotice(
  notice: ApiNotice,
  page: PageRuntimeResponse,
  trace: ApiResponse<PageRuntimeResponse>['trace'],
): WidgetDynamicError | null {
  const meta = notice.meta ?? {};
  if (meta['surface'] !== 'section') return null;

  const target = stringMeta(meta, 'target') ?? notice.target;
  const widgetId = target ? resolveWidgetId(page, target) : null;
  if (!widgetId) return null;

  return {
    widgetId,
    code: notice.code,
    severity: noticeSeverity(notice.severity),
    requestId: stringMeta(meta, 'requestId') ?? trace?.requestId,
    traceId: stringMeta(meta, 'traceId') ?? trace?.traceId,
    errorId: stringMeta(meta, 'errorId'),
  };
}

/**
 * PageModel notices name a stable functional slice. The resolved runtime config declares which
 * widget owns it, so the client never mirrors backend templates or component identifiers.
 * Direct widget ids remain accepted for resolver-generated widget failures during migration.
 */
function resolveWidgetId(page: PageRuntimeResponse, target: string): string | null {
  if (page.content.widgets[target]) return target;

  for (const [widgetId, config] of Object.entries(page.content.widgets)) {
    const feedbackTargets = config.props?.['feedbackTargets'];
    if (Array.isArray(feedbackTargets) && feedbackTargets.includes(target)) {
      return widgetId;
    }
  }
  return null;
}

/** Dynamic widget errors carry stable identifiers and support references, never transport prose. */
function sanitizeWidgetError(error: WidgetDynamicError): WidgetDynamicError {
  return {
    widgetId: error.widgetId,
    code: error.code,
    severity: error.severity,
    requestId: error.requestId,
    traceId: error.traceId,
    errorId: error.errorId,
  };
}

function stringMeta(meta: Readonly<Record<string, unknown>>, key: string): string | undefined {
  const value = meta[key];
  return typeof value === 'string' && value.trim() ? value : undefined;
}

function noticeSeverity(severity: ApiNotice['severity']): WidgetDynamicError['severity'] {
  if (severity === 'ERROR' || severity === 'error') return 'error';
  if (severity === 'WARN' || severity === 'warning') return 'warn';
  return 'info';
}

function langParams(lang?: string): HttpParams | undefined {
  return lang ? new HttpParams().set('lang', lang) : undefined;
}

function logicalIdParams(logicalId: string, lang?: string): HttpParams {
  let params = new HttpParams().set('logicalId', logicalId);
  if (lang) {
    params = params.set('lang', lang);
  }
  return params;
}
