import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { TchBackendClient } from '@tch/api';
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
    sendPasswordResetEmail: vi.fn(),
  };
  const runtime = {
    initialize: vi.fn(),
  };
  const backend = {
    post: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        AuthSessionService,
        { provide: AUTH_CLIENT, useValue: auth },
        { provide: PrivateRuntimeInitializer, useValue: runtime },
        { provide: TchBackendClient, useValue: backend },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
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
      permissions: ['draw-results.manual'],
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
    expect(session.permissions).toEqual(['draw-results.manual']);
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

  it('preserves tenant owner as a distinct role', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize.mockReturnValue(of({
      ...bootstrap(),
      entitlements: {
        roles: ['TENANT_OWNER'],
        permissions: [],
      },
    } satisfies RuntimeBootstrapResponse));

    const session = await TestBed.inject(AuthSessionService).refreshSession();

    expect(session.roles).toEqual(['TENANT_OWNER']);
  });

  it('exposes normalized permissions from private runtime entitlements', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize.mockReturnValue(of({
      ...bootstrap(),
      entitlements: {
        roles: ['tenant_admin'],
        permissions: [' Draw-Results.Manual ', 'draw-results.manual'],
      },
    } satisfies RuntimeBootstrapResponse));

    const service = TestBed.inject(AuthSessionService);
    const session = await service.refreshSession();

    expect(session.permissions).toEqual(['draw-results.manual']);
    expect(service.hasPermission('DRAW-RESULTS.MANUAL')).toBe(true);
  });

  it('refreshes runtime permissions every 30 minutes', async () => {
    vi.useFakeTimers();
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize
      .mockReturnValueOnce(of({
        ...bootstrap(),
        entitlements: { roles: ['tenant_admin'], permissions: ['ticket.read'] },
      } satisfies RuntimeBootstrapResponse))
      .mockReturnValueOnce(of({
        ...bootstrap(),
        entitlements: { roles: ['tenant_admin'], permissions: ['ticket.read', 'ticket.cancel'] },
      } satisfies RuntimeBootstrapResponse));

    const service = TestBed.inject(AuthSessionService);
    await service.refreshSession();

    expect(service.hasPermission('ticket.cancel')).toBe(false);

    await vi.advanceTimersByTimeAsync(30 * 60 * 1000);

    expect(runtime.initialize).toHaveBeenCalledTimes(2);
    expect(service.hasPermission('ticket.cancel')).toBe(true);
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

  it('delegates email login credentials directly to the configured auth client', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.login).mockResolvedValue(undefined);
    vi.mocked(auth.getAccessToken).mockResolvedValue('fresh-token');
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    runtime.initialize.mockReturnValue(of(bootstrap()));

    await TestBed.inject(AuthSessionService).login('admin@example.com', 'secret');

    expect(backend.post).not.toHaveBeenCalled();
    expect(auth.login).toHaveBeenCalledWith({
      username: 'admin@example.com',
      password: 'secret',
    });
    expect(auth.getAccessToken).toHaveBeenCalledWith(true);
  });

  it('resolves username login before calling the configured auth client', async () => {
    vi.mocked(auth.isAuthenticated).mockResolvedValue(true);
    vi.mocked(auth.login).mockResolvedValue(undefined);
    vi.mocked(auth.getAccessToken).mockResolvedValue('fresh-token');
    vi.mocked(auth.getTokenExpiresAt).mockResolvedValue(undefined);
    backend.post.mockReturnValue(of({ resolvedIdentifier: 'admin@example.com' }));
    runtime.initialize.mockReturnValue(of(bootstrap()));

    await TestBed.inject(AuthSessionService).login(' Admin ', 'secret');

    expect(backend.post).toHaveBeenCalledWith(
      '/public/auth/login-identifier/resolve',
      { identifier: 'admin' },
      { suppressShellFeedback: true },
    );
    expect(auth.login).toHaveBeenCalledWith({
      username: 'admin@example.com',
      password: 'secret',
    });
  });

  it('resolves username before sending a password reset email', async () => {
    backend.post.mockReturnValue(of({ resolvedIdentifier: 'admin@example.com' }));
    vi.mocked(auth.sendPasswordResetEmail!).mockResolvedValue(undefined);

    await TestBed.inject(AuthSessionService).sendPasswordResetEmail(' Admin ');

    expect(backend.post).toHaveBeenCalledWith(
      '/public/auth/login-identifier/resolve',
      { identifier: 'admin' },
      { suppressShellFeedback: true },
    );
    expect(auth.sendPasswordResetEmail).toHaveBeenCalledWith('admin@example.com');
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
    entitlements: { roles: ['tenant_admin'], permissions: ['draw-results.manual'] },
    readiness: { status: 'READY', checks: [] },
    notifications: { unreadCount: 0, criticalCount: 0 },
    pageModelRef: { route: '/app/admin', endpoint: '/pages/admin' },
  };
}
