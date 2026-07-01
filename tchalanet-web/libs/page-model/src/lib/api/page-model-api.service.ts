import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ApiNotice, ApiResponse, TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { PageRuntimeResponse, WidgetDynamicError } from '../runtime/pagemodel.types';

@Injectable({ providedIn: 'root' })
export class PageModelApi {
  private readonly backend = inject(TchBackendClient);

  getPublicPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.getApiResponse<PageRuntimeResponse>('/public/page', {
      params: langParams(lang),
      suppressShellFeedback: true,
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
}

function withSectionNotices(response: ApiResponse<PageRuntimeResponse>): PageRuntimeResponse {
  const existingKeys = new Set(
    (response.data?.dynamic?.errors ?? []).map(error => widgetErrorKey(error.widgetId, error.code)),
  );
  const widgetErrors = (response.notices ?? [])
    .map(notice => widgetErrorFromNotice(notice, response.trace))
    .filter((error): error is WidgetDynamicError => error !== null)
    .filter(error => !existingKeys.has(widgetErrorKey(error.widgetId, error.code)));

  if (!widgetErrors.length) return response.data;

  return {
    ...response.data,
    dynamic: {
      ...response.data.dynamic,
      errors: [
        ...(response.data.dynamic?.errors ?? []),
        ...widgetErrors,
      ],
    },
  };
}

function widgetErrorKey(widgetId: string, code: string | undefined): string {
  return `${widgetId}|${code ?? ''}`;
}

function widgetErrorFromNotice(
  notice: ApiNotice,
  trace: ApiResponse<PageRuntimeResponse>['trace'],
): WidgetDynamicError | null {
  const meta = notice.meta ?? {};
  if (meta['surface'] !== 'section') return null;

  const target = stringMeta(meta, 'target') ?? notice.target;
  if (!target) return null;

  return {
    widgetId: target,
    code: notice.code,
    message: notice.message,
    severity: noticeSeverity(notice.severity),
    requestId: stringMeta(meta, 'requestId') ?? trace?.requestId,
    traceId: stringMeta(meta, 'traceId') ?? trace?.traceId,
    errorId: stringMeta(meta, 'errorId'),
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
