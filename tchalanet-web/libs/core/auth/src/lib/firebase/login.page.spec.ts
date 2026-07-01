import '@angular/compiler';

import { TestBed } from '@angular/core/testing';

import { AuthRedirectService } from '../auth-redirect.service';
import { AuthSessionService } from '../auth-session.service';
import { LoginPage } from './login.page';
import { AUTH_CLIENT } from '../auth-client';
import { Router } from '@angular/router';
import { TchRuntimeConfigStore } from '@tch/shared-config';

describe('LoginPage', () => {
  it('redirects a restored admin provider session without bootstrapping private runtime on login', async () => {
    const authSession = {
      login: vi.fn(),
    };
    const authRedirect = {
      navigateAfterLogin: vi.fn().mockResolvedValue(undefined),
    };
    const authClient = {
      isAuthenticated: vi.fn().mockResolvedValue(true),
    };
    const router = {
      navigateByUrl: vi.fn().mockResolvedValue(true),
    };

    await configure(authSession, authRedirect, authClient, router);

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(authClient.isAuthenticated).toHaveBeenCalledOnce();
    expect(authSession.login).not.toHaveBeenCalled();
    expect(authRedirect.navigateAfterLogin).not.toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/admin');
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
    const router = {
      navigateByUrl: vi.fn().mockResolvedValue(true),
    };

    await configure(authSession, authRedirect, authClient, router);

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.email = 'admin@example.com';
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
  router: { navigateByUrl: ReturnType<typeof vi.fn> },
): Promise<void> {
  await TestBed.configureTestingModule({
    providers: [
      { provide: AuthSessionService, useValue: authSession },
      { provide: AuthRedirectService, useValue: authRedirect },
      { provide: AUTH_CLIENT, useValue: authClient },
      { provide: Router, useValue: router },
      {
        provide: TchRuntimeConfigStore,
        useValue: {
          config: () => ({ appId: 'admin-portal' }),
        },
      },
    ],
  });
}
