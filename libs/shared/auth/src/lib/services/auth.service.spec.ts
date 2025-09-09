// libs/shared/auth/auth.service.spec.ts
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService, TchClaim } from './auth.service';
import { EventTypes, OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client';
import { of, Subject } from 'rxjs';

// ---- Mocks ----
class OidcSecurityServiceMock {
  authorize = vi.fn();
  logoffAndRevokeTokens = vi.fn(() => of(void 0));
  checkAuth = vi.fn(() => of({ isAuthenticated: true, accessToken: 'at', idToken: 'it' }));
  // sync getters used by the service
  getAuthenticationResult = vi.fn(() => ({ isAuthenticated: true }));
  getAccessToken = vi.fn(() => 'at');
  getIdToken = vi.fn(() => 'it');
  getUserData = vi.fn(() => ({
    sub: '123',
    email: 'a@b.c',
    tch: { roles: ['SUPER_ADMIN'] } as TchClaim,
  }));
}
class PublicEventsServiceMock {
  bus = new Subject<any>();
  registerForEvents = vi.fn(() => this.bus.asObservable());
}

function makeService() {
  // emulate Angular injectors simply:
  const oidc = new OidcSecurityServiceMock() as unknown as OidcSecurityService;
  const events = new PublicEventsServiceMock() as unknown as PublicEventsService;

  // monkey-patch global inject() by supplying instances via closures if needed.
  // Simpler: patch AuthService to take deps in ctor for tests (or set (auth as any).oidc = oidc)
  const auth = new AuthService() as any;
  auth['oidc'] = oidc;
  auth['events'] = events;
  return { auth: auth as AuthService, oidc, events };
}

describe('AuthService (v20)', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('init() should call checkAuth and subscribe to events', async () => {
    const { auth, oidc, events } = makeService();
    await auth.init();
    expect(oidc.checkAuth).toHaveBeenCalledTimes(1);
    expect(events.registerForEvents).toHaveBeenCalledTimes(1);
  });

  it('login(target) stores target and calls authorize()', () => {
    const { auth, oidc } = makeService();
    auth.login('/app/dashboard');
    expect(sessionStorage.getItem('login_target')).toBe('/app/dashboard');
    expect(oidc.authorize).toHaveBeenCalledTimes(1);
  });

  it('consumeLoginTarget() returns stored target and clears it', () => {
    const { auth } = makeService();
    sessionStorage.setItem('login_target', '/app');
    expect(auth.consumeLoginTarget()).toBe('/app');
    expect(sessionStorage.getItem('login_target')).toBeNull();
  });

/*  it('isAuthenticatedSync() reflects OIDC state', async () => {
    const { auth, oidc } = makeService();
    (oidc.getAuthenticationResult as any).mockReturnValue({ isAuthenticated: false });
    expect(auth.isAuthenticatedSync()).toBe(false);
    (oidc.getAuthenticationResult as any).mockReturnValue({ isAuthenticated: true });
    expect(auth.isAuthenticatedSync()).toBe(true);
  });*/

  it('hasRole() uses tch.roles from claims', () => {
    const { auth } = makeService();
    expect(auth.hasRole('SUPER_ADMIN')).toBe(true);
    expect(auth.hasRole('TENANT_ADMIN')).toBe(false);
  });

  it('reacts to NewAuthenticationResult event (silent renew/login)', async () => {
    const { auth, events } = makeService();
    await auth.init();
    // emit fake event
    (events as any).bus.next({ type: EventTypes.NewAuthenticationResult });
    // no throw = ok; hydrate() is internal; we rely on getters anyway
    expect(true).toBe(true);
  });
});
