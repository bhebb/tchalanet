import { Injectable, computed, inject, signal } from '@angular/core';

import { I18nFacade } from '@tch/core/i18n';
import { ThemeStore } from '@tch/ui/theme';

import { PublicRuntimeInitializer } from './public-runtime-initializer';

type PublicRuntimeState = 'idle' | 'loading' | 'ready' | 'error';

@Injectable({ providedIn: 'root' })
export class PublicRuntimeStore {
  private readonly i18n = inject(I18nFacade);
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
    this.i18n.init();
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
