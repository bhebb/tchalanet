import { Injectable, computed, inject, signal } from '@angular/core';

import { AuthSessionService } from '../auth/auth-session.service';
import { I18nFacade } from '../i18n';
import { RuntimeSettingsStore } from '../settings';
import { ThemeRuntimeStore } from '../theme';

type RuntimeBootstrapState = 'idle' | 'loading' | 'ready';

@Injectable({ providedIn: 'root' })
export class AppRuntimeStore {
  private readonly auth = inject(AuthSessionService);
  private readonly i18n = inject(I18nFacade);
  private readonly settings = inject(RuntimeSettingsStore);
  private readonly theme = inject(ThemeRuntimeStore);
  private readonly bootstrapState = signal<RuntimeBootstrapState>('idle');

  readonly state = this.bootstrapState.asReadonly();
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
    if (this.bootstrapState() !== 'idle') {
      return;
    }

    this.bootstrapState.set('loading');
    this.i18n.init();
    this.theme.init();
    this.theme.loadPublicTheme();
    this.settings.loadPublicSettings();
    void this.auth.refreshSession().finally(() => this.bootstrapState.set('ready'));
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.settings.isFeatureEnabled(key, defaultValue);
  }
}
