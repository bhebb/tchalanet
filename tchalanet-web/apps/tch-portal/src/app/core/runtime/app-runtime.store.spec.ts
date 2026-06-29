import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthSessionService, PrivateRuntimeInitializer } from '@tch/core/auth';
import { ThemeStore } from '@tch/ui/theme';
import { of } from 'rxjs';

import { FeatureFlags } from '@tch/shared-config';
import { I18nFacade } from '@tch/core/i18n';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { AppRuntimeStore } from './app-runtime.store';
import { PublicRuntimeInitializer } from '@tch/web/shell';

describe('AppRuntimeStore', () => {
  const anonymousSession = {
    authenticated: false,
    roles: [],
  };
  const authenticatedSession = {
    authenticated: true,
    roles: ['CASHIER'] as const,
  };

  let auth: {
    session: ReturnType<typeof signal<typeof anonymousSession>>;
    refreshSession: ReturnType<typeof vi.fn>;
  };
  let i18n: { currentLanguage: ReturnType<typeof signal<string>>; init: ReturnType<typeof vi.fn> };
  let settings: {
    loadState: ReturnType<typeof signal<'idle' | 'loading' | 'ready' | 'fallback'>>;
    ready: ReturnType<typeof signal<boolean>>;
    settings: ReturnType<typeof signal<Record<string, unknown>>>;
    isFeatureEnabled: ReturnType<typeof vi.fn>;
  };
  let theme: {
    activeTheme: ReturnType<typeof signal<Record<string, unknown>>>;
    loadState: ReturnType<typeof signal<'idle' | 'loading' | 'ready' | 'fallback'>>;
    init: ReturnType<typeof vi.fn>;
  };
  let privateInitializer: { initialize: ReturnType<typeof vi.fn> };
  let publicInitializer: { initialize: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    auth = {
      session: signal(anonymousSession),
      refreshSession: vi.fn().mockResolvedValue(anonymousSession),
    };
    i18n = {
      currentLanguage: signal('fr'),
      init: vi.fn(),
    };
    settings = {
      isFeatureEnabled: vi.fn(),
      loadState: signal('idle'),
      ready: signal(true),
      settings: signal({}),
    };
    theme = {
      activeTheme: signal({}),
      init: vi.fn(),
      loadState: signal('ready'),
    };
    privateInitializer = {
      initialize: vi.fn(() => of(undefined)),
    };
    publicInitializer = {
      initialize: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [
        AppRuntimeStore,
        { provide: AuthSessionService, useValue: auth },
        { provide: I18nFacade, useValue: i18n },
        { provide: RuntimeSettingsStore, useValue: settings },
        {
          provide: FeatureFlags,
          useValue: { isEnabled: (key: string, fallback = false) => fallback },
        },
        { provide: ThemeStore, useValue: theme },
        { provide: PrivateRuntimeInitializer, useValue: privateInitializer },
        { provide: PublicRuntimeInitializer, useValue: publicInitializer },
      ],
    });
  });

  it('initializes public runtime via a single public bootstrap call', async () => {
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPublicRuntime();
    await flushPromises();

    expect(i18n.init).toHaveBeenCalledOnce();
    expect(theme.init).toHaveBeenCalledOnce();
    expect(auth.refreshSession).toHaveBeenCalledOnce();
    // Single public bootstrap call: no separate settings/theme runtime calls (bootstrap carries them).
    expect(publicInitializer.initialize).toHaveBeenCalledOnce();
    expect(runtime.scope()).toBe('public');
  });

  it('runs the private bootstrap only after an authenticated refresh', async () => {
    auth.refreshSession.mockResolvedValue(authenticatedSession);
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPrivateRuntime();
    expect(privateInitializer.initialize).not.toHaveBeenCalled();
    await flushPromises();

    expect(auth.refreshSession).toHaveBeenCalledOnce();
    expect(privateInitializer.initialize).toHaveBeenCalledOnce();
    expect(publicInitializer.initialize).not.toHaveBeenCalled();
    expect(runtime.scope()).toBe('private');
  });

  it('falls back to the public bootstrap when a private refresh is anonymous', async () => {
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPrivateRuntime();
    await flushPromises();

    expect(publicInitializer.initialize).toHaveBeenCalledOnce();
    expect(privateInitializer.initialize).not.toHaveBeenCalled();
  });

  it('keeps repeated public and private bootstrap calls idempotent', async () => {
    auth.refreshSession.mockResolvedValue(authenticatedSession);
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPublicRuntime();
    runtime.initPublicRuntime();
    await flushPromises();
    runtime.initPrivateRuntime();
    runtime.initPrivateRuntime();
    await flushPromises();

    expect(auth.refreshSession).toHaveBeenCalledTimes(2);
    expect(publicInitializer.initialize).toHaveBeenCalledOnce();
    expect(privateInitializer.initialize).toHaveBeenCalledOnce();
    expect(runtime.scope()).toBe('private');
  });

  it('does not let a pending public refresh overwrite an upgraded private bootstrap', async () => {
    let resolvePublicRefresh: (session: typeof anonymousSession) => void = () => undefined;
    const publicRefresh = new Promise<typeof anonymousSession>(resolve => {
      resolvePublicRefresh = resolve;
    });
    auth.refreshSession
      .mockReturnValueOnce(publicRefresh)
      .mockResolvedValueOnce(authenticatedSession);
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPublicRuntime();
    runtime.initPrivateRuntime();
    await flushPromises();
    resolvePublicRefresh(anonymousSession);
    await flushPromises();

    expect(privateInitializer.initialize).toHaveBeenCalledOnce();
    expect(publicInitializer.initialize).not.toHaveBeenCalled();
    expect(runtime.scope()).toBe('private');
  });

  it('records bootstrap errors and exposes error state', async () => {
    const error = new Error('session refresh failed');
    auth.refreshSession.mockRejectedValue(error);
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPrivateRuntime();
    await flushPromises();

    expect(runtime.state()).toBe('error');
    expect(runtime.error()).toBe(error);
    // On refresh failure it still attempts the public bootstrap as a best-effort fallback.
    expect(publicInitializer.initialize).toHaveBeenCalledOnce();
  });
});

async function flushPromises(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}
