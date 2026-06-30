import { TestBed } from '@angular/core/testing';
import { TchBackendClient } from '@tch/api';
import { of } from 'rxjs';

import { PrivateBootstrapService } from './private-bootstrap.service';

describe('PrivateBootstrapService', () => {
  it('uses the canonical private runtime endpoint', () => {
    const backend = { get: vi.fn(() => of(bootstrap())) };
    TestBed.configureTestingModule({
      providers: [PrivateBootstrapService, { provide: TchBackendClient, useValue: backend }],
    });

    TestBed.inject(PrivateBootstrapService).bootstrap().subscribe();

    expect(backend.get).toHaveBeenCalledWith('/runtime/private', {
      suppressShellFeedback: true,
    });
  });

  it('returns the unwrapped runtime bootstrap data', () => {
    const backend = { get: vi.fn(() => of(bootstrap())) };
    TestBed.configureTestingModule({
      providers: [PrivateBootstrapService, { provide: TchBackendClient, useValue: backend }],
    });

    TestBed.inject(PrivateBootstrapService)
      .bootstrap()
      .subscribe(response => expect(response.user?.username).toBe('runtime-user'));
  });
});

function bootstrap() {
  return {
    space: 'PLATFORM',
    user: {
      userId: 'user-1',
      username: 'runtime-user',
      displayName: 'Runtime User',
      email: 'runtime@example.com',
      roles: ['SUPER_ADMIN'],
      defaultSpace: 'PLATFORM',
      preferredLocale: 'fr',
      preferredTimezone: 'America/Toronto',
    },
    tenantContext: null,
    entitlements: { roles: ['SUPER_ADMIN'], permissions: [] },
    readiness: { status: 'READY', checks: [] },
    notifications: { unreadCount: 0, criticalCount: 0 },
    pageModelRef: { route: '/app/platform', endpoint: '/pages/platform' },
    entryRoute: '/app/platform',
  };
}
