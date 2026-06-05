import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { PlatformAdminApi } from './platform-admin-api.service';
import { SuperAdminDashboardPage } from './super-admin-dashboard.page';

describe('SuperAdminDashboardPage', () => {
  const api = {
    previewTenant: vi.fn(),
    provisionTenant: vi.fn(),
  };

  function setup() {
    api.previewTenant.mockReset();
    api.provisionTenant.mockReset();
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), { provide: PlatformAdminApi, useValue: api }],
    });
    return TestBed.createComponent(SuperAdminDashboardPage).componentInstance;
  }

  it('does not provision while the form is invalid (missing code/name)', () => {
    const cmp = setup();
    expect(cmp.form.invalid).toBe(true);
    cmp.provision();
    expect(api.provisionTenant).not.toHaveBeenCalled();
  });

  it('provisions a tenant with the initial admin email and stores the result', () => {
    const cmp = setup();
    api.provisionTenant.mockReturnValue(
      of({ tenantId: 't1', tenantCode: 'ACME', initialAdminUserId: 'u1' }),
    );
    cmp.form.setValue({
      code: 'ACME',
      name: 'Acme',
      type: 'BORLETTE',
      profile: 'MINIMAL',
      timezone: 'America/Port-au-Prince',
      currency: 'HTG',
      initialAdminEmail: 'admin@acme.test',
    });

    cmp.provision();

    expect(api.provisionTenant).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'ACME', initialAdminEmail: 'admin@acme.test' }),
    );
    expect(cmp.result()?.tenantCode).toBe('ACME');
    expect(cmp.error()).toBe(false);
  });

  it('flags an error when provisioning fails', () => {
    const cmp = setup();
    api.provisionTenant.mockReturnValue(throwError(() => new Error('boom')));
    cmp.form.patchValue({ code: 'X', name: 'X' });

    cmp.provision();

    expect(cmp.error()).toBe(true);
    expect(cmp.result()).toBeNull();
  });
});
