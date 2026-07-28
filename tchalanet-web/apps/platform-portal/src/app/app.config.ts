import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideState, provideStore } from '@ngrx/store';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import {
  TCH_API_BASE_RESOLVER,
  correlationRequestInterceptor,
  problemDetailInterceptor,
} from '@tch/api';
import {
  PrivateBootstrapStore,
  authBearerInterceptor,
  footerFromRuntimeNavigation,
  provideFirebaseAuthClient,
  sectionsFromRuntimeNavigation,
  supportAccessInterceptor,
} from '@tch/core/auth';
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
import {
  PLATFORM_FOOTER,
  PLATFORM_NAVIGATION,
  TCH_TITLE_NAVIGATION,
  provideTchTitleStrategy,
  shellFeedbackInterceptor,
} from '@tch/web/shell';

import { appRoutes } from './app.routes';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
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
    // Le titre d'onglet doit venir de la **même** source que le menu affiché : la navigation
    // runtime du backend, avec le modèle statique en repli.
    {
      provide: TCH_TITLE_NAVIGATION,
      useFactory: () => {
        const bootstrap = inject(PrivateBootstrapStore);
        return () => {
          const drawer = bootstrap.navigationDrawer();
          const sections = sectionsFromRuntimeNavigation(drawer) ?? PLATFORM_NAVIGATION;
          const footer = footerFromRuntimeNavigation(drawer) ?? PLATFORM_FOOTER;
          // Le bas de menu porte aussi des destinations : sans lui, ces pages n'auraient pas
          // de titre d'onglet.
          return footer.length
            ? [...sections, { id: 'footer', titleKey: '', items: footer }]
            : sections;
        };
      },
    },
    ...provideTchTitleStrategy(),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationRequestInterceptor,
        problemDetailInterceptor,
        shellFeedbackInterceptor,
        authBearerInterceptor,
        supportAccessInterceptor,
      ]),
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
