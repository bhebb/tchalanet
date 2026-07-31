import '@angular/compiler';

import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TCH_FEEDBACK_CONTEXT } from '@tch/api';

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
    expect(publicRequest.request.context.get(TCH_FEEDBACK_CONTEXT)).toEqual({
      owner: 'feature',
      mode: 'local',
      target: 'page-model',
    });
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

  it('passes tenant dashboard period and performance page to the backend', () => {
    api.getTenantPage(undefined, 'YESTERDAY', 2).subscribe();

    http
      .expectOne('/api/v1/tenant/dashboard?period=YESTERDAY&performancePage=2')
      .flush(response());
  });

  it('loads the private dashboard fallback from bundled assets', () => {
    let result: unknown;
    api.getPrivateFallbackPage().subscribe(page => {
      result = page.meta.logicalId;
    });

    http.expectOne('/assets/config/page-private-fallback.json').flush({
      meta: {
        logicalId: 'private.dashboard.fallback',
        scope: 'private',
        slug: 'dashboard',
        schemaVersion: 2,
      },
      shell: { type: 'private', topAppBar: {}, navigationDrawer: {} },
      content: { layout: { rows: [] }, widgets: {} },
      dynamic: { widgets: {}, errors: [] },
    });

    expect(result).toBe('private.dashboard.fallback');
  });

  it('rejects successful PageModel responses without data', () => {
    let failed = false;
    api.getTenantPage().subscribe({ error: () => { failed = true; } });

    http.expectOne('/api/v1/tenant/dashboard').flush({
      status: 'SUCCESS',
      data: null,
      notices: [],
      services: [],
    });

    expect(failed).toBe(true);
  });

  it('maps section-targeted ApiResponse notices into widget-local dynamic errors', () => {
    let resultErrors: unknown;
    api.getPlatformPage().subscribe(result => {
      resultErrors = result.dynamic.errors;
    });

    http
      .expectOne('/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin')
      .flush(response({
        widgets: { 'dashboard.commissions': { type: 'CommissionSummaryWidget' } },
        notices: [
          {
            code: 'dashboard.commissions.unavailable',
            message: 'Commissions are temporarily unavailable.',
            domain: 'dashboard',
            severity: 'WARN',
            kind: 'DEGRADATION',
            target: 'dashboard.commissions',
            trace: { traceId: 'trace-1', errorId: 'err-1' },
            meta: {
              target: 'legacy-target',
              traceId: 'legacy-trace',
              errorId: 'legacy-error',
            },
          },
        ],
      }));

    expect(resultErrors).toEqual([
      {
        widgetId: 'dashboard.commissions',
        code: 'dashboard.commissions.unavailable',
        severity: 'warn',
        traceId: 'trace-1',
        errorId: 'err-1',
      },
    ]);
  });

  it('keeps a tenant dashboard degradation owned by its rendered widget', () => {
    let resultErrors: unknown;
    api.getTenantPage().subscribe(result => {
      resultErrors = result.dynamic.errors;
    });

    http.expectOne('/api/v1/tenant/dashboard').flush(response({
      notices: [
        {
          code: 'tenantadmin.dashboard.analytics_unavailable',
          severity: 'WARN',
          meta: {
            surface: 'section',
            target: 'tenant_admin_dashboard.analytics',
          },
        },
      ],
      widgets: {
        'dashboard.tenantAdmin.salesTrend': {
          type: 'TrendChartWidget',
          props: { feedbackTargets: ['tenant_admin_dashboard.analytics'] },
        },
      },
    }));

    expect(resultErrors).toEqual([
      {
        widgetId: 'dashboard.tenantAdmin.salesTrend',
        code: 'tenantadmin.dashboard.analytics_unavailable',
        severity: 'warn',
      },
    ]);
  });

  it('does not retain raw notice or backend widget messages in the render model', () => {
    let resultErrors: unknown;
    api.getPlatformPage().subscribe(result => {
      resultErrors = result.dynamic.errors;
    });

    http
      .expectOne(
        '/api/v1/platform/dashboard?logicalId=private.dashboard.superadmin',
      )
      .flush(response({
        widgets: { 'dashboard.commissions': { type: 'CommissionSummaryWidget' } },
        notices: [
          {
            code: 'dashboard.commissions.unavailable',
            message: 'provider payload: secret diagnostic',
            severity: 'WARN',
            meta: { surface: 'section', target: 'dashboard.commissions' },
          },
        ],
        errors: [
          {
            widgetId: 'dashboard.sales',
            code: 'dashboard.sales.unavailable',
            message: 'internal backend error',
          },
        ],
      }));

    expect(resultErrors).toEqual([
      { widgetId: 'dashboard.sales', code: 'dashboard.sales.unavailable' },
      {
        widgetId: 'dashboard.commissions',
        code: 'dashboard.commissions.unavailable',
        severity: 'warn',
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
        widgets: { 'dashboard.commissions': { type: 'CommissionSummaryWidget' } },
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

function response(overrides: {
  notices?: readonly unknown[];
  errors?: readonly unknown[];
  widgets?: Readonly<Record<string, unknown>>;
} = {}) {
  return {
    status: 'SUCCESS',
    data: {
      meta: { logicalId: 'public.home', scope: 'public', slug: 'home', schemaVersion: 2 },
      shell: { type: 'public', header: {}, footer: {} },
      content: { layout: { rows: [] }, widgets: overrides.widgets ?? {} },
      dynamic: { widgets: {}, errors: overrides.errors ?? [] },
    },
    notices: overrides.notices ?? [],
    services: [],
  };
}
