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
    localStorage.clear();
    vi.unstubAllGlobals();
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

  it('hides the install prompt when the app is already running standalone', async () => {
    mockMobileViewport(true);
    mockStandaloneMode(true);

    await configure(
      { refreshSession: vi.fn(), login: vi.fn() },
      { navigateAfterLogin: vi.fn() },
      { isAuthenticated: vi.fn().mockResolvedValue(false) },
    );

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();

    expect(page.installHintVisible()).toBe(false);
  });

  it('dismisses the iPhone install hint when the user returns after seeing instructions', async () => {
    mockMobileViewport(true);
    mockStandaloneMode(false);
    mockUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)');

    await configure(
      { refreshSession: vi.fn(), login: vi.fn() },
      { navigateAfterLogin: vi.fn() },
      { isAuthenticated: vi.fn().mockResolvedValue(false) },
    );

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();

    expect(page.installHintVisible()).toBe(true);

    await page.installApp();
    expect(page.infoKey()).toBe('auth.login.install.iosHint');

    window.dispatchEvent(new Event('focus'));

    expect(page.installHintVisible()).toBe(false);
    expect(page.infoKey()).toBeNull();
    expect(localStorage.getItem('tch-install-hint-dismissed')).toBe('true');
  });

  it('uses Android-specific install instructions when the native prompt is unavailable', async () => {
    mockMobileViewport(true);
    mockStandaloneMode(false);
    mockUserAgent('Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36');

    await configure(
      { refreshSession: vi.fn(), login: vi.fn() },
      { navigateAfterLogin: vi.fn() },
      { isAuthenticated: vi.fn().mockResolvedValue(false) },
    );

    const page = TestBed.runInInjectionContext(() => new LoginPage());
    page.ngOnInit();

    await page.installApp();

    expect(page.infoKey()).toBe('auth.login.install.androidHint');
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

function mockMobileViewport(matches: boolean): void {
  vi.stubGlobal(
    'matchMedia',
    vi.fn().mockImplementation((query: string) => ({
      matches: query === '(max-width: 599px)' ? matches : false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  );
}

function mockStandaloneMode(standalone: boolean): void {
  Object.defineProperty(window.navigator, 'standalone', {
    configurable: true,
    value: standalone,
  });
}

function mockUserAgent(userAgent: string): void {
  Object.defineProperty(window.navigator, 'userAgent', {
    configurable: true,
    value: userAgent,
  });
}
