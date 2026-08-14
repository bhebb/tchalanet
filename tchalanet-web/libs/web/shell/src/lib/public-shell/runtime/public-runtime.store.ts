import { InjectionToken, Injectable, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs';

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
  private readonly language$ = toObservable(this.i18n.currentLanguage);
  private readonly stateSignal = signal<PublicRuntimeState>('idle');
  private readonly errorSignal = signal<unknown | null>(null);
  private initialized = false;

  readonly state = this.stateSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly ready = computed(() => this.stateSignal() === 'ready');

  init(): void {
    if (this.initialized) {
      return;
    }

    this.initialized = true;
    this.stateSignal.set('loading');
    this.errorSignal.set(null);
    this.i18n.init(this.i18nConfig.languages, this.i18nConfig.defaultLanguage);
    this.theme.init();

    this.language$
      .pipe(
        filter(language => language.length > 0),
        distinctUntilChanged(),
        tap(() => {
          this.stateSignal.set('loading');
          this.errorSignal.set(null);
        }),
        switchMap(language =>
          this.initializer.initialize(language).pipe(
            tap(() => this.stateSignal.set('ready')),
            catchError((error: unknown) => {
              this.errorSignal.set(error);
              this.stateSignal.set('error');
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe();
  }
}
