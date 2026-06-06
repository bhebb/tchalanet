import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PageModelApi } from './page-model-api.service';

describe('PageModelApi', () => {
  let api: PageModelApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PageModelApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the surface-oriented runtime routes', () => {
    api.getPublicPage().subscribe();
    http.expectOne('/api/v1/public/page').flush(response());

    api.getTenantPage().subscribe();
    http.expectOne('/api/v1/tenant/dashboard').flush(response());

    api.getPlatformPage().subscribe();
    http.expectOne('/api/v1/platform/dashboard').flush(response());
  });
});

function response() {
  return {
    status: 'SUCCESS',
    data: {
      meta: { logicalId: 'public.home', scope: 'public', slug: 'home', schemaVersion: 2 },
      shell: { type: 'public', header: {}, footer: {} },
      content: { layout: { rows: [] }, widgets: {} },
      dynamic: { widgets: {}, errors: [] },
    },
    notices: [],
    services: [],
  };
}
