import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPage as TchPageResult } from '../../operations/data-access/platform-ops-api.service';

export interface PageModelTemplateView {
  id: string;
  code: string;
  logicalId: string;
  scope: string;
  slug: string;
  name: string;
  label: string;
  description?: string;
  isDefault: boolean;
  level: 'GLOBAL' | 'TENANT';
  tenantId?: { value: string } | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformPageModelsApi {
  private readonly backend = inject(TchBackendClient);

  listTemplates(params: { q?: string; scope?: string; page?: number; size?: number }): Observable<TchPageResult<PageModelTemplateView>> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<TchPageResult<PageModelTemplateView>>(`/platform/page-model-templates${q ? '?' + q : ''}`);
  }

  getTemplate(id: string): Observable<PageModelTemplateView> {
    return this.backend.get<PageModelTemplateView>(`/platform/page-model-templates/${id}`);
  }

  setDefault(id: string): Observable<PageModelTemplateView> {
    return this.backend.post<PageModelTemplateView>(`/platform/page-model-templates/${id}/default`, {});
  }

  duplicate(id: string): Observable<PageModelTemplateView> {
    return this.backend.post<PageModelTemplateView>(`/platform/page-model-templates/${id}/duplicate`, {});
  }

  deleteTemplate(id: string): Observable<void> {
    return this.backend.delete<void>(`/platform/page-model-templates/${id}`);
  }
}
