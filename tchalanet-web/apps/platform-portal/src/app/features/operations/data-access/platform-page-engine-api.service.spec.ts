// @vitest-environment jsdom
import '@angular/compiler';

import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TCH_FEEDBACK_CONTEXT } from '@tch/api';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { PlatformPageEngineApi } from './platform-page-engine-api.service';

describe('PlatformPageEngineApi', () => {
  let api: PlatformPageEngineApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    api = TestBed.inject(PlatformPageEngineApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the platform PageModel route', async () => {
    const resource = api.listResource(() => ({ page: 0, size: 20 }));
    TestBed.tick();

    const request = http.expectOne(candidate => candidate.url === '/api/v1/platform/pagemodels');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush({
      status: 'SUCCESS',
      data: {
        items: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
        hasNext: false,
        hasPrevious: false,
      },
      notices: [],
      services: [],
    });

    await Promise.resolve();
    TestBed.tick();
    expect(resource.value()?.items).toEqual([]);
  });

  it('sends the selected tenant context for platform writes', () => {
    api
      .publish('page-1', {
        suppressShellFeedback: true,
        asTenantAdmin: { tenantId: 'tenant-1', reason: 'test' },
      })
      .subscribe();

    const request = http.expectOne('/api/v1/platform/pagemodels/page-1/publish');
    expect(request.request.headers.get('X-Tch-Tenant-Override')).toBe('tenant-1');
    expect(request.request.headers.get('X-Tch-Act-As')).toBe('TENANT_ADMIN');
    expect(request.request.context.get(TCH_FEEDBACK_CONTEXT)).toEqual({ mode: 'local' });
    request.flush({ status: 'SUCCESS', data: true, notices: [], services: [] });
  });
});
