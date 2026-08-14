import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import {
  TCH_API_BASE_RESOLVER,
  correlationRequestInterceptor,
  problemDetailInterceptor,
} from '@tch/api';
import { authBearerInterceptor, provideFirebaseAuthClient } from '@tch/core/auth';
import {
  I18nEffects,
  MERGED_TRANSLATE_LOADER_OPTIONS,
  MergedTranslateLoader,
  TCH_I18N_LANGUAGE_STORAGE_KEY,
  i18nFeature,
  provideTchAngularLocales,
} from '@tch/core/i18n';
import {
  FeatureFlags,
  PORTAL_I18N_CONFIG,
  SettingsFeatureFlags,
  TchRuntimeConfigStore,
  provideTchRuntimeConfig,
} from '@tch/shared-config';
import { themeStoreProvider } from '@tch/ui/theme';
import { provideWidgets } from '@tch/widgets';
import {
  PUBLIC_RUNTIME_I18N_CONFIG,
  provideTchTitleStrategy,
  shellFeedbackInterceptor,
} from '@tch/web/shell';
import { provideEffects } from '@ngrx/effects';
import { provideState, provideStore } from '@ngrx/store';

import { appRoutes } from './app.routes';
import { environment } from '../environments/environment';

const PUBLIC_PORTAL_I18N_CONFIG = {
  ...PORTAL_I18N_CONFIG,
  fallbackLang: 'fr',
  defaultLang: 'fr',
} as const;

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration(withEventReplay()),
    provideBrowserGlobalErrorListeners(),
    provideTchRuntimeConfig({
      fallback: environment.fallbackConfig,
      runtimeConfigPath: environment.runtimeConfigPath,
    }),
    {
      provide: TCH_API_BASE_RESOLVER,
      useFactory: (runtimeConfig: TchRuntimeConfigStore) => () =>
        runtimeConfig.config().apiBaseUrl,
      deps: [TchRuntimeConfigStore],
    },
    provideRouter(appRoutes),
    ...provideTchTitleStrategy(),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationRequestInterceptor,
        problemDetailInterceptor,
        shellFeedbackInterceptor,
        authBearerInterceptor,
      ]),
    ),
    provideAnimationsAsync(),
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    provideStore({}),
    provideState(i18nFeature),
    provideEffects([I18nEffects]),
    { provide: TCH_I18N_LANGUAGE_STORAGE_KEY, useValue: 'tchalanet.web.public.language' },
    {
      provide: PUBLIC_RUNTIME_I18N_CONFIG,
      useValue: {
        languages: ['fr', 'en', 'ht'],
        defaultLanguage: PUBLIC_PORTAL_I18N_CONFIG.defaultLang,
      },
    },
    themeStoreProvider,
    provideWidgets(),
    provideFirebaseAuthClient({
      options: environment.fallbackConfig.firebase,
      emulatorUrl: environment.fallbackConfig.firebaseAuthEmulatorUrl,
    }),
    { provide: FeatureFlags, useExisting: SettingsFeatureFlags },
    provideTchAngularLocales(PUBLIC_PORTAL_I18N_CONFIG.defaultLang),
    provideTranslateService({
      fallbackLang: PUBLIC_PORTAL_I18N_CONFIG.fallbackLang,
      lang: PUBLIC_PORTAL_I18N_CONFIG.defaultLang,
      loader: {
        provide: TranslateLoader,
        useClass: MergedTranslateLoader,
      },
    }),
    {
      provide: MERGED_TRANSLATE_LOADER_OPTIONS,
      useValue: {
        assetsPrefix: PUBLIC_PORTAL_I18N_CONFIG.assetsPrefix,
        assetsSuffix: PUBLIC_PORTAL_I18N_CONFIG.assetsSuffix,
        bundles: PUBLIC_PORTAL_I18N_CONFIG.bundles,
      },
    },
  ],
};
