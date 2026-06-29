import { TestBed } from '@angular/core/testing';
import { TchBackendClient } from '@tch/api';
import { of } from 'rxjs';

import { PrivateBootstrapService } from './private-bootstrap.service';

describe('PrivateBootstrapService', () => {
  it('uses the canonical private runtime endpoint', () => {
    const backend = { get: vi.fn(() => of(undefined)) };
    TestBed.configureTestingModule({
      providers: [PrivateBootstrapService, { provide: TchBackendClient, useValue: backend }],
    });

    TestBed.inject(PrivateBootstrapService).bootstrap().subscribe();

    expect(backend.get).toHaveBeenCalledWith('/tenant/runtime/bootstrap', {
      suppressShellFeedback: true,
    });
  });
});
