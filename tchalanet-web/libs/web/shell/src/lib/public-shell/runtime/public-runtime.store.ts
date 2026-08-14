import { InjectionToken, Injectable, computed, inject, signal } from '@angular/core';

import { I18nFacade } from '@tch/core/i18n';
import { ThemeStore } from '@tch/ui/theme';

import { PublicRuntimeInitializer } from './public-runtime-initializer';

type PublicRuntimeState = 'idle' | 'loading' | 'ready' | 'error';

export interface PublicRuntimeI18nConfig {
  readonly languages: readonly string[];
  readonly defaultLanguage: string;
}

export const PUBLIC_RUNTIME_I18N_CONFIG = new InjectionToken<PublicRuntimeI18nConfig>(
  'PUBLIC_RUNTIME_I18N_CONFIG',
  {
    factory: () => ({
      languages: ['fr', 'en', 'ht'],
      defaultLanguage: 'ht',
    }),
  },
);

@Injectable({ providedIn: 'root' })
export class PublicRuntimeStore {
  private readonly i18n = inject(I18nFacade);
  private readonly i18nConfig = inject(PUBLIC_RUNTIME_I18N_CONFIG);
  private readonly theme = inject(ThemeStore);
  private readonly initializer = inject(PublicRuntimeInitializer);
  private readonly stateSignal = signal<PublicRuntimeState>('idle');
  private readonly errorSignal = signal<unknown | null>(null);

  readonly state = this.stateSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly ready = computed(() => this.stateSignal() === 'ready');

  init(): void {
    if (this.stateSignal() !== 'idle') {
      return;
    }

    this.stateSignal.set('loading');
    this.errorSignal.set(null);
    this.i18n.init(this.i18nConfig.languages, this.i18nConfig.defaultLanguage);
    this.theme.init();

    this.initializer.initialize(this.i18n.currentLanguage()).subscribe({
      next: () => this.stateSignal.set('ready'),
      error: (error: unknown) => {
        this.errorSignal.set(error);
        this.stateSignal.set('error');
      },
    });
  }
}
