import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  isDevMode,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideRouterStore } from '@ngrx/router-store';
import { provideState, provideStore } from '@ngrx/store';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { TranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { themeRuntimeStoreProvider } from '@tch/shared/theme/runtime/theme-runtime.store';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
} from 'keycloak-angular';

import { appRoutes } from './app.routes';
import { FeatureFlags, SettingsFeatureFlags } from './core/feature';
import {
  correlationRequestInterceptor,
  problemDetailInterceptor,
} from './core/http';
import {
  I18nEffects,
  MERGED_TRANSLATE_LOADER_OPTIONS,
  MergedTranslateLoader,
  i18nFeature,
} from './core/i18n';

const localApiUrlPattern =
  /^(\/api\/|https?:\/\/(localhost|127\.0\.0\.1):8083\/api\/|https?:\/\/api\.(localtest\.me|tchalanet\.lan)\/api\/)/i;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(appRoutes),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        correlationRequestInterceptor,
        includeBearerTokenInterceptor,
        problemDetailInterceptor,
      ]),
    ),
    provideAnimationsAsync(),
    provideStore({}),
    provideState(i18nFeature),
    provideEffects([I18nEffects]),
    provideRouterStore(),
    themeRuntimeStoreProvider,
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
      fallbackLang: 'fr',
      lang: 'fr',
      loader: {
        provide: TranslateLoader,
        useClass: MergedTranslateLoader,
      },
    }),
    {
      provide: MERGED_TRANSLATE_LOADER_OPTIONS,
      useValue: {
        assetsPrefix: '/assets/i18n/',
        assetsSuffix: '.json',
        backendPath: '/api/v1/public/i18n',
        surfaces: ['PUBLIC_COMMON', 'PUBLIC_HOME'],
      },
    },
    provideKeycloak({
      config: {
        url: keycloakUrl(),
        realm: 'tchalanet',
        clientId: 'tchalanet-web',
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
          urlPattern: localApiUrlPattern,
        },
      ],
    },
  ],
};

function keycloakUrl(): string {
  const hostname = globalThis.location?.hostname ?? '';

  if (hostname.endsWith('tchalanet.lan')) {
    return 'https://auth.tchalanet.lan';
  }

  return 'https://auth.localtest.me';
}
