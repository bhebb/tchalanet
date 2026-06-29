import { DatePipe, DecimalPipe, registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {import { ApplicationConfig, ErrorHandler, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';

ApplicationConfig,
  ErrorHandler,
  isDevMode,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';import { provideRouter } from '@angular/router';

import { AppErrorHandler } from './core/error/app-error-handler';
import { provideEffects } from '@ngrx/effects';
import { provideRouterStore } from '@ngrx/router-store';
import { provideState, provideStore } from '@ngrx/store';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { correlationRequestInterceptor, problemDetailInterceptor } from '@tch/api';
import { FeatureFlags, PORTAL_I18N_CONFIG, SettingsFeatureFlags } from '@tch/shared-config';
import { themeStoreProvider } from '@tch/ui/theme';
import { provideWidgets } from '@tch/widgets';
import {
  authBearerInterceptor,
  provideFirebaseAuthClient,
  supportAccessInterceptor,
} from '@tch/core/auth';
import { appRoutes } from './app.routes';
import {
  I18nEffects,
  MERGED_TRANSLATE_LOADER_OPTIONS,
  MergedTranslateLoader,
  i18nFeature,
} from '@tch/core/i18n';
import { FeatureFlags, PORTAL_I18N_CONFIG, SettingsFeatureFlags } from '@tch/shared-config';
import { themeStoreProvider } from '@tch/ui/theme';
import { provideWidgets } from '@tch/widgets';

import { appRoutes } from './app.routes';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { environment } from '../environments/environment';

import { apiFeedbackInterceptor } from './core/api/api-feedback.interceptor';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: ErrorHandler, useClass: AppErrorHandler },
    { provide: LOCALE_ID, useValue: 'fr' },
    DatePipe,
    DecimalPipe,
    provideRouter(appRoutes),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationRequestInterceptor,
        authBearerInterceptor,
        supportAccessInterceptor,
        apiFeedbackInterceptor,
        problemDetailInterceptor,
      ]),
    ),
    provideAnimationsAsync(),
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    provideStore({}),
    provideState(i18nFeature),
    provideEffects([I18nEffects]),
    provideRouterStore(),
    themeStoreProvider,
    provideWidgets(),

    provideFirebaseAuthClient({
      options: environment.firebase,
      emulatorUrl: environment.firebaseAuthEmulatorUrl,
    }),
    // Feature-management isolation seam: call sites depend on FeatureFlags, swapping the backing
    // provider (e.g. to Unleash) only rebinds this token.
    { provide: FeatureFlags, useExisting: SettingsFeatureFlags },
    ...(isDevMode()
      ? [
          provideStoreDevtools({
            maxAge: 25,
            name: 'Tchalanet Web',
          }),
        ]
      : []),
    provideTranslateService({
      fallbackLang: PORTAL_I18N_CONFIG.fallbackLang,
      lang: PORTAL_I18N_CONFIG.defaultLang,
      loader: {
        provide: TranslateLoader,
        useClass: MergedTranslateLoader,
      },
    }),
    {
      provide: MERGED_TRANSLATE_LOADER_OPTIONS,
      useValue: {
        assetsPrefix: PORTAL_I18N_CONFIG.assetsPrefix,
        assetsSuffix: PORTAL_I18N_CONFIG.assetsSuffix,
        bundles: PORTAL_I18N_CONFIG.bundles,
      },
    },
  ],
};
