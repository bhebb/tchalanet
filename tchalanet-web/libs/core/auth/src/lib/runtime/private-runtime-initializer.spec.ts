import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { ThemeStore } from '@tch/ui/theme';
import { of } from 'rxjs';

import { RuntimeBootstrapResponse } from './private-bootstrap.model';
import { PrivateRuntimeInitializer } from './private-runtime-initializer';
import { PrivateBootstrapService } from './private-bootstrap.service';
import { PrivateBootstrapStore } from './private-bootstrap.store';

describe('PrivateRuntimeInitializer', () => {
  const bootstrapApi = {
    bootstrap: vi.fn(),
  };
  const bootstrapStore = {
    setLoading: vi.fn(),
    setBootstrap: vi.fn(),
  };
  const translate = {
    setTranslation: vi.fn(),
  };
  const theme = {
    applyBootstrapTheme: vi.fn(),
  };
  const settings = {
    applyBootstrapSettings: vi.fn(),
  };

  beforeEach(() => {
    bootstrapApi.bootstrap.mockReset();
    bootstrapStore.setLoading.mockReset();
    bootstrapStore.setBootstrap.mockReset();
    translate.setTranslation.mockReset();
    theme.applyBootstrapTheme.mockReset();
    settings.applyBootstrapSettings.mockReset();

    TestBed.configureTestingModule({
      providers: [
        PrivateRuntimeInitializer,
        { provide: PrivateBootstrapService, useValue: bootstrapApi },
        { provide: PrivateBootstrapStore, useValue: bootstrapStore },
        { provide: TranslateService, useValue: translate },
        { provide: ThemeStore, useValue: theme },
        { provide: RuntimeSettingsStore, useValue: settings },
      ],
    });
  });

  it('keeps private bootstrap usable when optional runtime blocks are missing', () => {
    bootstrapApi.bootstrap.mockReturnValue(of(bootstrapWithoutRuntimeBlocks()));

    TestBed.inject(PrivateRuntimeInitializer).initialize().subscribe();

    expect(theme.applyBootstrapTheme).toHaveBeenCalledWith({
      presetCode: undefined,
      mode: undefined,
      tokens: undefined,
    });
    expect(settings.applyBootstrapSettings).toHaveBeenCalledWith({
      locale: 'fr',
      timezone: 'America/Toronto',
      currency: 'HTG',
      features: {},
    });
    expect(translate.setTranslation).not.toHaveBeenCalled();
    expect(bootstrapStore.setBootstrap).toHaveBeenCalled();
  });
});

function bootstrapWithoutRuntimeBlocks(): RuntimeBootstrapResponse {
  return {
    space: 'PLATFORM',
    user: {
      userId: 'user-1',
      username: 'super-admin',
      displayName: 'Super Admin',
      email: 'super-admin@example.com',
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
