import { Injectable, computed, inject, signal } from '@angular/core';
import { ThemeStore } from '@tch/ui/theme';

import { AuthSessionService } from '../auth/auth-session.service';
import { FeatureFlags } from '@tch/shared-config';
import { I18nFacade } from '../i18n';
import { RuntimeSettingsStore } from '@tch/shared-config';

type RuntimeBootstrapState = 'idle' | 'loading' | 'ready' | 'error';
type RuntimeBootstrapScope = 'none' | 'public' | 'private';

@Injectable({ providedIn: 'root' })
export class AppRuntimeStore {
  private readonly auth = inject(AuthSessionService);
  private readonly i18n = inject(I18nFacade);
  private readonly settings = inject(RuntimeSettingsStore);
  private readonly features = inject(FeatureFlags);
  private readonly theme = inject(ThemeStore);
  private readonly bootstrapState = signal<RuntimeBootstrapState>('idle');
  private readonly bootstrapScope = signal<RuntimeBootstrapScope>('none');
  private readonly bootstrapError = signal<unknown | null>(null);

  readonly state = this.bootstrapState.asReadonly();
  readonly scope = this.bootstrapScope.asReadonly();
  readonly error = this.bootstrapError.asReadonly();
  readonly currentLanguage = this.i18n.currentLanguage;
  readonly connectedUser = computed(() => this.auth.session());
  readonly currentTheme = this.theme.activeTheme;
  readonly runtimeSettings = this.settings.settings;
  readonly settingsState = this.settings.loadState;
  readonly themeState = this.theme.loadState;
  readonly ready = computed(
    () =>
      this.bootstrapState() === 'ready' &&
      this.settings.ready() &&
      (this.theme.loadState() === 'ready' || this.theme.loadState() === 'fallback'),
  );

  initPublicRuntime(): void {
    if (this.bootstrapScope() !== 'none') {
      return;
    }

    this.bootstrapScope.set('public');
    this.bootstrapState.set('loading');
    this.bootstrapError.set(null);
    this.i18n.init();
    this.theme.init();
    void this.auth
      .refreshSession()
      .catch((error: unknown) => this.bootstrapError.set(error))
      .finally(() => {
        if (this.bootstrapScope() !== 'public') {
          return;
        }

        this.theme.loadPublicTheme();
        this.settings.loadPublicSettings();
        this.bootstrapState.set(this.bootstrapError() ? 'error' : 'ready');
      });
  }

  initPrivateRuntime(): void {
    if (this.bootstrapScope() === 'private') {
      return;
    }

    this.bootstrapScope.set('private');
    this.bootstrapState.set('loading');
    this.bootstrapError.set(null);
    this.i18n.init();
    this.theme.init();
    void this.auth
      .refreshSession()
      .then(session => {
        if (session.authenticated) {
          this.theme.loadPrivateTheme();
          this.settings.loadPrivateSettings();
          return;
        }

        this.theme.loadPublicTheme();
        this.settings.loadPublicSettings();
      })
      .catch((error: unknown) => {
        this.bootstrapError.set(error);
        this.theme.loadPublicTheme();
        this.settings.loadPublicSettings();
      })
      .finally(() => this.bootstrapState.set(this.bootstrapError() ? 'error' : 'ready'));
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.features.isEnabled(key, defaultValue);
  }
}
