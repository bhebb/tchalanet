import '@angular/compiler';

import { TestBed } from '@angular/core/testing';

import { AuthRedirectService } from '../auth-redirect.service';
import { AuthSessionService } from '../auth-session.service';
import { LoginPage } from './login.page';
import { AUTH_CLIENT } from '../auth-client';

describe('LoginPage', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    sessionStorage.clear();
  });

  it('redirects a restored tenant admin session through the post-login dispatcher', async () => {
    const session = {
      authenticated: true,
      roles: ['TENANT_ADMIN' as const],
    };
    const authSession = {
      refreshSession: vi.fn().mockResolvedValue(session),
      login: vi.fn(),
    };
    const authRedirect = {
      navigateAfterLogin: vi.fn().mockResolvedValue(undefined),
    };
    const authClient = {
      isAuthenticated: vi.fn().mockResolvedValue(true),
    };

    await configure(authSession, authRedirect, authClient);

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(authClient.isAuthenticated).toHaveBeenCalledOnce();
    expect(authSession.refreshSession).toHaveBeenCalledWith(true);
    expect(authSession.login).not.toHaveBeenCalled();
    expect(authRedirect.navigateAfterLogin).toHaveBeenCalledWith(session);
  });

  it('redirects a restored super admin session through the post-login dispatcher', async () => {
    const session = {
      authenticated: true,
      roles: ['SUPER_ADMIN' as const],
    };
    const authSession = {
      refreshSession: vi.fn().mockResolvedValue(session),
      login: vi.fn(),
    };
    const authRedirect = {
      navigateAfterLogin: vi.fn().mockResolvedValue(undefined),
    };
    const authClient = {
      isAuthenticated: vi.fn().mockResolvedValue(true),
    };

    await configure(authSession, authRedirect, authClient);

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(authClient.isAuthenticated).toHaveBeenCalledOnce();
    expect(authSession.refreshSession).toHaveBeenCalledWith(true);
    expect(authSession.login).not.toHaveBeenCalled();
    expect(authRedirect.navigateAfterLogin).toHaveBeenCalledWith(session);
  });

  it('redirects after a successful credential login', async () => {
    const session = {
      authenticated: true,
      roles: ['TENANT_ADMIN' as const],
    };
    const authSession = {
      refreshSession: vi.fn().mockResolvedValue({ authenticated: false, roles: [] }),
      login: vi.fn().mockResolvedValue(session),
    };
    const authRedirect = {
      navigateAfterLogin: vi.fn().mockResolvedValue(undefined),
    };
    const authClient = {
      isAuthenticated: vi.fn().mockResolvedValue(false),
    };

    await configure(authSession, authRedirect, authClient);

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.identifier = 'admin@example.com';
    page.password = 'secret';

    await page.submit();

    expect(authSession.login).toHaveBeenCalledWith('admin@example.com', 'secret');
    expect(authRedirect.navigateAfterLogin).toHaveBeenCalledWith(session);
    expect(page.loading()).toBe(false);
  });
});

async function configure(
  authSession: Partial<AuthSessionService>,
  authRedirect: Partial<AuthRedirectService>,
  authClient: { isAuthenticated: ReturnType<typeof vi.fn> },
): Promise<void> {
  await TestBed.configureTestingModule({
    providers: [
      { provide: AuthSessionService, useValue: authSession },
      { provide: AuthRedirectService, useValue: authRedirect },
      { provide: AUTH_CLIENT, useValue: authClient },
    ],
  });
}
