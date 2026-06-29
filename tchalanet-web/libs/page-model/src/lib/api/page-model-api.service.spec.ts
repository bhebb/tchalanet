import '@angular/compiler';

import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SUPPRESS_SHELL_FEEDBACK } from '@tch/api';

import { PageModelApi } from './page-model-api.service';

describe('PageModelApi', () => {
  let api: PageModelApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()],
    });
    api = TestBed.inject(PageModelApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the surface-oriented runtime routes', () => {
    api.getPublicPage().subscribe();
    const publicRequest = http.expectOne('/api/v1/public/page');
    expect(publicRequest.request.context.get(SUPPRESS_SHELL_FEEDBACK)).toBe(true);
    publicRequest.flush(response());

    api.getTenantPage().subscribe();
    http.expectOne('/api/v1/tenant/dashboard').flush(response());

    api.getPlatformPage().subscribe();
    http
      .expectOne(
        '/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin',
      )
      .flush(response());
  });

  it('can request a platform page by logicalId', () => {
    api.getPlatformPage('private.dashboard.superadmin.ops').subscribe();

    http
      .expectOne(
        '/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin.ops',
      )
      .flush(response());
  });

  it('maps section-targeted ApiResponse notices into widget-local dynamic errors', () => {
    let resultErrors: unknown;
    api.getPlatformPage().subscribe(result => {
      resultErrors = result.dynamic.errors;
    });

    http
      .expectOne('/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin')
      .flush(response({
        notices: [
          {
            code: 'dashboard.commissions.unavailable',
            message: 'Commissions are temporarily unavailable.',
            domain: 'dashboard',
            severity: 'WARN',
            meta: {
              surface: 'section',
              placement: 'top',
              target: 'dashboard.commissions',
              traceId: 'trace-1',
              errorId: 'err-1',
            },
          },
        ],
      }));

    expect(resultErrors).toEqual([
      {
        widgetId: 'dashboard.commissions',
        code: 'dashboard.commissions.unavailable',
        message: 'Commissions are temporarily unavailable.',
        severity: 'warn',
        traceId: 'trace-1',
        errorId: 'err-1',
      },
    ]);
  });

  it('does not duplicate widget errors already present in the PageModel data', () => {
    let resultErrors: unknown;
    api.getPlatformPage().subscribe(result => {
      resultErrors = result.dynamic.errors;
    });

    http
      .expectOne('/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin')
      .flush(response({
        errors: [
          {
            widgetId: 'dashboard.commissions',
            code: 'dashboard.commissions.unavailable',
          },
        ],
        notices: [
          {
            code: 'dashboard.commissions.unavailable',
            message: 'Commissions are temporarily unavailable.',
            domain: 'dashboard',
            severity: 'WARN',
            meta: {
              surface: 'section',
              placement: 'top',
              target: 'dashboard.commissions',
            },
          },
        ],
      }));

    expect(resultErrors).toEqual([
      {
        widgetId: 'dashboard.commissions',
        code: 'dashboard.commissions.unavailable',
      },
    ]);
  });
});

function response(overrides: { notices?: readonly unknown[]; errors?: readonly unknown[] } = {}) {
  return {
    status: 'SUCCESS',
    data: {
      meta: { logicalId: 'public.home', scope: 'public', slug: 'home', schemaVersion: 2 },
      shell: { type: 'public', header: {}, footer: {} },
      content: { layout: { rows: [] }, widgets: {} },
      dynamic: { widgets: {}, errors: overrides.errors ?? [] },
    },
    notices: overrides.notices ?? [],
    services: [],
  };
}
