import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthSessionService, PrivateRuntimeInitializer } from '@tch/core/auth';
import { ThemeStore } from '@tch/ui/theme';

import { FeatureFlags } from '@tch/shared-config';
import { I18nFacade } from '@tch/core/i18n';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { PublicRuntimeInitializer } from '@tch/web/shell';

type RuntimeBootstrapState = 'idle' | 'loading' | 'ready' | 'error';
type RuntimeBootstrapScope = 'none' | 'public' | 'private';

@Injectable({ providedIn: 'root' })
export class AppRuntimeStore {
  private readonly auth = inject(AuthSessionService);
  private readonly i18n = inject(I18nFacade);
  private readonly settings = inject(RuntimeSettingsStore);
  private readonly features = inject(FeatureFlags);
  private readonly theme = inject(ThemeStore);
  private readonly privateInitializer = inject(PrivateRuntimeInitializer);
  private readonly publicInitializer = inject(PublicRuntimeInitializer);
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

        // Single public runtime call: bootstrap carries settings + theme + i18n + navigation +
        // pageModelRef. No separate settings/theme HTTP calls (only bootstrap + the page call).
        this.publicInitializer.initialize(this.i18n.currentLanguage()).subscribe({
          error: (err: unknown) => this.bootstrapError.set(err),
        });
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
          // Single private runtime call: bootstrap carries settings + theme + i18n + nav + readiness.
          this.privateInitializer.initialize().subscribe({
            error: (err: unknown) => this.bootstrapError.set(err),
          });
          return;
        }

        // Anonymous on a private route: fall back to the public bootstrap (still one runtime call).
        this.publicInitializer.initialize(this.i18n.currentLanguage()).subscribe({
          error: (err: unknown) => this.bootstrapError.set(err),
        });
      })
      .catch((error: unknown) => {
        this.bootstrapError.set(error);
        this.publicInitializer.initialize(this.i18n.currentLanguage()).subscribe();
      })
      .finally(() => this.bootstrapState.set(this.bootstrapError() ? 'error' : 'ready'));
  }

  isFeatureEnabled(key: string, defaultValue = false): boolean {
    return this.features.isEnabled(key, defaultValue);
  }
}
