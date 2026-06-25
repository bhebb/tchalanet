import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthSessionService } from './auth-session.service';
import { spaceDispatchGuard } from './auth.guard';
import { UserRole, UserSession } from './auth.types';

describe('spaceDispatchGuard', () => {
  const auth = {
    refreshSession: vi.fn(),
  };
  const router = {
    parseUrl: vi.fn((url: string) => url),
  };

  beforeEach(() => {
    auth.refreshSession.mockReset();
    router.parseUrl.mockClear();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthSessionService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  it.each([
    ['SUPER_ADMIN', '/app/platform'],
    ['TENANT_ADMIN', '/app/admin'],
    ['CASHIER', '/app/cashier'],
  ] as const)('redirects %s to %s', async (role: UserRole, expected: string) => {
    auth.refreshSession.mockResolvedValue(session([role]));

    const result = await TestBed.runInInjectionContext(() =>
      spaceDispatchGuard({} as never, {} as never),
    );

    expect(result).toBe(expected);
  });

  it('redirects an anonymous user to login', async () => {
    auth.refreshSession.mockResolvedValue(session([]));

    const result = await TestBed.runInInjectionContext(() =>
      spaceDispatchGuard({} as never, {} as never),
    );

    expect(result).toBe('/login');
  });
});

function session(roles: readonly UserRole[]): UserSession {
  return {
    authenticated: roles.length > 0,
    roles,
  };
}
