import { TestBed } from '@angular/core/testing';
import { TchBackendClient } from '@tch/api';
import { of } from 'rxjs';

import { AdminFinancialsApi } from './admin-financials-api.service';

describe('AdminFinancialsApi', () => {
  it('uses the tenant-scoped backend context without passing tenantId from the UI', () => {
    const backend = { get: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({
      providers: [AdminFinancialsApi, { provide: TchBackendClient, useValue: backend }],
    });

    TestBed.inject(AdminFinancialsApi)
      .getBreakdown({ from: '2026-06-25', to: '2026-06-25' })
      .subscribe();

    expect(backend.get).toHaveBeenCalledWith('/admin/financials/breakdown', {
      params: {
        from: '2026-06-25',
        to: '2026-06-25',
        drawLimit: '100',
        sellerTerminalLimit: '100',
      },
    });
    const [, options] = backend.get.mock.calls[0] as unknown as [
      string,
      { params: Record<string, string> },
    ];
    expect(options.params).not.toHaveProperty('tenantId');
  });
});
