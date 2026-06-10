import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideRouterStore } from '@ngrx/router-store';
import { provideState, provideStore } from '@ngrx/store';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { correlationRequestInterceptor, problemDetailInterceptor } from '@tch/api';
import { apiFeedbackInterceptor } from './shared/api/api-feedback.interceptor';
import {
  APPLICATION_API_URL_PATTERN,
  AUTH_CONFIG,
  FeatureFlags,
  PORTAL_I18N_CONFIG,
  SettingsFeatureFlags,
  keycloakUrlForHostname,
} from '@tch/shared-config';
import { themeStoreProvider } from '@tch/ui/theme';
import { provideWidgets } from '@tch/widgets';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
} from 'keycloak-angular';

import { appRoutes } from './app.routes';
import {
  I18nEffects,
  MERGED_TRANSLATE_LOADER_OPTIONS,
  MergedTranslateLoader,
  i18nFeature,
} from './core/i18n';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(appRoutes),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationRequestInterceptor,
        includeBearerTokenInterceptor,
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
        backendPath: PORTAL_I18N_CONFIG.backendPath,
        surfaces: PORTAL_I18N_CONFIG.surfaces,
      },
    },
    provideKeycloak({
      config: {
        url: keycloakUrlForHostname(globalThis.location?.hostname ?? ''),
        realm: AUTH_CONFIG.realm,
        clientId: AUTH_CONFIG.clientId,
      },
      initOptions: {
        onLoad: 'check-sso',
        checkLoginIframe: false,
      },
    }),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: [
        {
          urlPattern: APPLICATION_API_URL_PATTERN,
        },
      ],
    },
  ],
};
