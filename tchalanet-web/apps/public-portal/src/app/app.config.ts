import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { TCH_API_BASE, correlationRequestInterceptor, problemDetailInterceptor } from '@tch/api';
import { provideFirebaseAuthClient } from '@tch/core/auth';
import {
  I18nEffects,
  MERGED_TRANSLATE_LOADER_OPTIONS,
  MergedTranslateLoader,
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
import { provideEffects } from '@ngrx/effects';
import { provideState, provideStore } from '@ngrx/store';

import { appRoutes } from './app.routes';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideClientHydration(withEventReplay()),
    provideBrowserGlobalErrorListeners(),
    provideTchRuntimeConfig({
      fallback: environment.fallbackConfig,
      runtimeConfigPath: environment.runtimeConfigPath,
    }),
    {
      provide: TCH_API_BASE,
      useFactory: (runtimeConfig: TchRuntimeConfigStore) => runtimeConfig.config().apiBaseUrl,
      deps: [TchRuntimeConfigStore],
    },
    provideRouter(appRoutes),
    provideHttpClient(
      withFetch(),
      withInterceptors([correlationRequestInterceptor, problemDetailInterceptor]),
    ),
    provideAnimationsAsync(),
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    provideStore({}),
    provideState(i18nFeature),
    provideEffects([I18nEffects]),
    themeStoreProvider,
    provideWidgets(),
    provideFirebaseAuthClient({
      options: environment.fallbackConfig.firebase,
      emulatorUrl: environment.fallbackConfig.firebaseAuthEmulatorUrl,
    }),
    { provide: FeatureFlags, useExisting: SettingsFeatureFlags },
    provideTchAngularLocales(PORTAL_I18N_CONFIG.defaultLang),
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
