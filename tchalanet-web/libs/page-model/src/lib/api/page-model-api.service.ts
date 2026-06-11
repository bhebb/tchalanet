import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

import { PageRuntimeResponse } from '../runtime/pagemodel.types';

@Injectable({ providedIn: 'root' })
export class PageModelApi {
  private readonly backend = inject(TchBackendClient);

  getPublicPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.get<PageRuntimeResponse>('/public/page', { params: langParams(lang) });
  }

  getPublicManagersPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.get<PageRuntimeResponse>('/public/managers', { params: langParams(lang) });
  }

  getPlatformPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.get<PageRuntimeResponse>('/platform/dashboard', {
      params: langParams(lang),
    });
  }

  getTenantPage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.get<PageRuntimeResponse>('/tenant/dashboard', { params: langParams(lang) });
  }

  getCashierHomePage(lang?: string): Observable<PageRuntimeResponse> {
    return this.backend.get<PageRuntimeResponse>('/tenant/cashier/home', {
      params: langParams(lang),
    });
  }
}

function langParams(lang?: string): HttpParams | undefined {
  return lang ? new HttpParams().set('lang', lang) : undefined;
}
