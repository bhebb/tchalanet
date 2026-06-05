import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { TenantAdminApi } from './tenant-admin-api.service';
import { TenantAdminDashboardPage } from './tenant-admin-dashboard.page';

describe('TenantAdminDashboardPage', () => {
  const api = { createSeller: vi.fn() };

  function setup() {
    api.createSeller.mockReset();
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), { provide: TenantAdminApi, useValue: api }],
    });
    return TestBed.createComponent(TenantAdminDashboardPage).componentInstance;
  }

  it('blocks submission when outletId is missing (CASHIER requires an outlet)', () => {
    const cmp = setup();
    cmp.form.patchValue({ email: 'seller@acme.test' });
    expect(cmp.form.invalid).toBe(true);
    cmp.submit();
    expect(api.createSeller).not.toHaveBeenCalled();
  });

  it('onboards a seller with role CASHIER and the outlet id', () => {
    const cmp = setup();
    api.createSeller.mockReturnValue(of({ id: 'u9', email: 'seller@acme.test' }));
    cmp.form.patchValue({ email: 'seller@acme.test', outletId: 'outlet-1' });

    cmp.submit();

    expect(api.createSeller).toHaveBeenCalledWith(
      expect.objectContaining({ email: 'seller@acme.test', role: 'CASHIER', outletId: 'outlet-1' }),
    );
    expect(cmp.result()?.id).toBe('u9');
  });

  it('does not expose a tenant id field in the form', () => {
    const cmp = setup();
    expect(Object.keys(cmp.form.controls)).not.toContain('tenantId');
  });
});
