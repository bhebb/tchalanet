import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ThemeStore } from '@tch/ui/theme';
import { of } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { FeatureFlags } from '@tch/shared-config';
import { I18nFacade } from '../i18n';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { AppRuntimeStore } from './app-runtime.store';

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
    loadPublicSettings: ReturnType<typeof vi.fn>;
    loadPrivateSettings: ReturnType<typeof vi.fn>;
    isFeatureEnabled: ReturnType<typeof vi.fn>;
  };
  let theme: {
    activeTheme: ReturnType<typeof signal<Record<string, unknown>>>;
    loadState: ReturnType<typeof signal<'idle' | 'loading' | 'ready' | 'fallback'>>;
    init: ReturnType<typeof vi.fn>;
    loadPublicTheme: ReturnType<typeof vi.fn>;
    loadPrivateTheme: ReturnType<typeof vi.fn>;
  };

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
      loadPrivateSettings: vi.fn(),
      loadPublicSettings: vi.fn(),
      loadState: signal('idle'),
      ready: signal(true),
      settings: signal({}),
    };
    theme = {
      activeTheme: signal({}),
      init: vi.fn(),
      loadPrivateTheme: vi.fn(() => of(null)),
      loadPublicTheme: vi.fn(() => of(null)),
      loadState: signal('ready'),
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
      ],
    });
  });

  it('initializes public runtime and loads public settings/theme after session refresh', async () => {
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPublicRuntime();
    await flushPromises();

    expect(i18n.init).toHaveBeenCalledOnce();
    expect(theme.init).toHaveBeenCalledOnce();
    expect(auth.refreshSession).toHaveBeenCalledOnce();
    expect(theme.loadPublicTheme).toHaveBeenCalledOnce();
    expect(settings.loadPublicSettings).toHaveBeenCalledOnce();
    expect(runtime.scope()).toBe('public');
  });

  it('loads private settings/theme only after authenticated refresh', async () => {
    auth.refreshSession.mockResolvedValue(authenticatedSession);
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPrivateRuntime();
    expect(settings.loadPrivateSettings).not.toHaveBeenCalled();
    await flushPromises();

    expect(auth.refreshSession).toHaveBeenCalledOnce();
    expect(theme.loadPrivateTheme).toHaveBeenCalledOnce();
    expect(settings.loadPrivateSettings).toHaveBeenCalledOnce();
    expect(theme.loadPublicTheme).not.toHaveBeenCalled();
    expect(settings.loadPublicSettings).not.toHaveBeenCalled();
    expect(runtime.scope()).toBe('private');
  });

  it('falls back to public settings/theme when private refresh is anonymous', async () => {
    const runtime = TestBed.inject(AppRuntimeStore);

    runtime.initPrivateRuntime();
    await flushPromises();

    expect(theme.loadPublicTheme).toHaveBeenCalledOnce();
    expect(settings.loadPublicSettings).toHaveBeenCalledOnce();
    expect(theme.loadPrivateTheme).not.toHaveBeenCalled();
    expect(settings.loadPrivateSettings).not.toHaveBeenCalled();
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
    expect(settings.loadPublicSettings).toHaveBeenCalledOnce();
    expect(settings.loadPrivateSettings).toHaveBeenCalledOnce();
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

    expect(settings.loadPrivateSettings).toHaveBeenCalledOnce();
    expect(theme.loadPrivateTheme).toHaveBeenCalledOnce();
    expect(settings.loadPublicSettings).not.toHaveBeenCalled();
    expect(theme.loadPublicTheme).not.toHaveBeenCalled();
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
    expect(settings.loadPublicSettings).toHaveBeenCalledOnce();
    expect(theme.loadPublicTheme).toHaveBeenCalledOnce();
  });
});

async function flushPromises(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}
