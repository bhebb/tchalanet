import { Injectable, ResourceRef, inject } from '@angular/core';
import { TchBackendClient, TchPage, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type PageModelStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type PageModelId = string | { value: string };

export interface PageModelSummaryView {
  readonly id: PageModelId;
  readonly logicalId: string;
  readonly scope: string;
  readonly slug: string;
  readonly status: PageModelStatus;
  readonly schemaVersion: number;
  readonly updatedAt: string;
}

export interface PageModelAdminDetailView {
  readonly id: string;
  readonly tenantId: string | null;
  readonly logicalId: string;
  readonly scope: string;
  readonly slug: string;
  readonly status: PageModelStatus;
  readonly schemaVersion: number;
  readonly model: unknown;
  readonly templateId: string | null;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
  readonly createdBy: string | null;
  readonly updatedBy: string | null;
  readonly publishedAt: string | null;
}

export interface PageModelUpsertRequest {
  readonly logicalId: string;
  readonly scope: string;
  readonly slug: string;
  readonly schemaVersion: number;
  readonly model: unknown;
}

export interface PageModelListParams {
  readonly q?: string;
  readonly scope?: string;
  readonly logicalId?: string;
  readonly page?: number;
  readonly size?: number;
}

@Injectable({ providedIn: 'root' })
export class PlatformPageEngineApi {
  private readonly backend = inject(TchBackendClient);

  listResource(
    params: () => PageModelListParams,
    options?: TchRequestOptions,
  ): ResourceRef<TchPage<PageModelSummaryView> | undefined> {
    return this.backend.getPageResource<PageModelSummaryView>(() => ({
      path: '/admin/pagemodels',
      options: {
        ...options,
        params: pageModelQueryParams(params()),
      },
    }));
  }

  previewResource(
    id: () => string | undefined,
    options?: TchRequestOptions,
  ): ResourceRef<PageModelAdminDetailView | undefined> {
    return this.backend.getResource<PageModelAdminDetailView>(() => {
      const pageModelId = id();
      return pageModelId
        ? {
            path: `/admin/pagemodels/${pageModelId}/preview`,
            options,
          }
        : undefined;
    });
  }

  create(req: PageModelUpsertRequest): Observable<PageModelAdminDetailView> {
    return this.backend.post<PageModelAdminDetailView>('/admin/pagemodels', req);
  }

  update(id: string, req: PageModelUpsertRequest): Observable<PageModelAdminDetailView> {
    return this.backend.put<PageModelAdminDetailView>(`/admin/pagemodels/${id}`, req);
  }

  publish(id: string): Observable<boolean> {
    return this.backend.post<boolean>(`/admin/pagemodels/${id}/publish`, {});
  }

  duplicate(id: string, logicalId?: string, slug?: string): Observable<PageModelAdminDetailView> {
    const params = new URLSearchParams();
    if (logicalId) params.set('logicalId', logicalId);
    if (slug) params.set('slug', slug);
    const query = params.toString();
    return this.backend.post<PageModelAdminDetailView>(
      `/admin/pagemodels/${id}/duplicate${query ? '?' + query : ''}`,
      {},
    );
  }

  reset(id: string): Observable<PageModelAdminDetailView> {
    return this.backend.post<PageModelAdminDetailView>(`/admin/pagemodels/${id}/reset`, {});
  }
}

export function pageModelIdValue(id: PageModelId): string {
  return typeof id === 'string' ? id : id.value;
}

function pageModelQueryParams(params: PageModelListParams): Record<string, string> {
  return {
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
    ...(params.scope ? { scope: params.scope } : {}),
    ...(params.logicalId ? { logicalId: params.logicalId } : {}),
    ...(params.q ? { logicalId: params.q } : {}),
  };
}
