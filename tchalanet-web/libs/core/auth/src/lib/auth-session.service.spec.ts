import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RuntimeBootstrapResponse } from './runtime/private-bootstrap.model';
import { PrivateRuntimeInitializer } from './runtime/private-runtime-initializer';
import { AUTH_CLIENT, AuthClient } from './auth-client';
import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  const auth: AuthClient = {
    isAuthenticated: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    getAccessToken: vi.fn(),
    getTokenExpiresAt: vi.fn(),
  };
  const runtime = {
    initialize: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AuthSessionService,
        { provide: AUTH_CLIENT, useValue: auth },
        { provide: PrivateRuntimeInitializer, useValue: runtime },
      ],
    });
  });

  it('builds the application session from private runtime data', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue('2026-06-13T20:00:00.000Z');
    runtime.initialize.mockReturnValue(of(bootstrap()));

    const session = await TestBed.inject(AuthSessionService).refreshSession();

    expect(session).toEqual({
      authenticated: true,
      userId: 'user-1',
      username: 'runtime-user',
      displayName: 'Runtime User',
      tenantId: 'tenant-1',
      tenantCode: 'TCH',
      roles: ['TENANT_ADMIN'],
      tokenExpiresAt: '2026-06-13T20:00:00.000Z',
      entryRoute: '/app/admin',
      mustChangePassword: false,
      mustCompleteProfile: false,
    });
  });

  it('keeps an authenticated provider session restored after browser refresh', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue('2026-06-13T20:00:00.000Z');
    runtime.initialize.mockReturnValue(of(bootstrap()));

    const session = await TestBed.inject(AuthSessionService).refreshSession();

    expect(auth.isAuthenticated).toHaveBeenCalledOnce();
    expect(runtime.initialize).toHaveBeenCalledOnce();
    expect(session.authenticated).toBe(true);
    expect(session.roles).toEqual(['TENANT_ADMIN']);
  });

  it('derives the platform role from the private runtime space', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize.mockReturnValue(of({
      ...bootstrap(),
      space: 'PLATFORM',
      tenantContext: null,
      entitlements: { roles: ['PLATFORM_ADMIN'], permissions: [] },
      user: {
        ...bootstrap().user,
        roles: [],
        defaultSpace: 'PLATFORM',
      },
      entryRoute: '/app/platform',
      pageModelRef: { route: '/app/platform', endpoint: '/pages/platform' },
    } satisfies RuntimeBootstrapResponse));

    const session = await TestBed.inject(AuthSessionService).refreshSession();

    expect(session.roles).toEqual(['SUPER_ADMIN']);
    expect(session.entryRoute).toBe('/app/platform');
  });

  it('does not bootstrap private runtime when provider session is anonymous', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(false);

    const session = await TestBed.inject(AuthSessionService).refreshSession();

    expect(session).toEqual({
      authenticated: false,
      roles: [],
    });
    expect(runtime.initialize).not.toHaveBeenCalled();
  });

  it('delegates login credentials and persistence choice to the configured auth client', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize.mockReturnValue(of(bootstrap()));

    await TestBed.inject(AuthSessionService).login('admin@example.com', 'secret', false);

    expect(auth.login).toHaveBeenCalledWith({
      username: 'admin@example.com',
      password: 'secret',
      remember: false,
    });
  });

  it('delegates logout and clears the application session', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    runtime.initialize.mockReturnValue(of(bootstrap()));
    const service = TestBed.inject(AuthSessionService);
    await service.refreshSession();

    await service.logout();

    expect(auth.logout).toHaveBeenCalledOnce();
    expect(service.session()).toEqual({
      authenticated: false,
      roles: [],
    });
  });
});

function bootstrap(): RuntimeBootstrapResponse {
  return {
    space: 'ADMIN',
    user: {
      userId: 'user-1',
      username: 'runtime-user',
      displayName: 'Runtime User',
      email: 'provider-email@example.com',
      roles: [],
      defaultSpace: 'ADMIN',
      preferredLocale: null,
      preferredTimezone: null,
    },
    tenantContext: {
      tenantId: 'tenant-1',
      tenantCode: 'TCH',
      tenantName: 'Tchalanet',
    },
    settings: { locale: 'fr', timezone: 'America/Toronto', currency: 'HTG', features: {} },
    theme: { presetCode: 'default', mode: 'light', tokens: {}, isDefault: true, version: 1 },
    i18n: { locale: 'fr', messages: {} },
    entitlements: { roles: ['tenant_admin'], permissions: [] },
    readiness: { status: 'READY', checks: [] },
    notifications: { unreadCount: 0, criticalCount: 0 },
    pageModelRef: { route: '/app/admin', endpoint: '/pages/admin' },
  };
}
