import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { AbstractSecurityStorage, LogLevel, provideAuth } from 'angular-auth-oidc-client';
import { provideMarkdown } from 'ngx-markdown';

import { HttpClient, provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  APP_INITIALIZER,
  ApplicationConfig,
  InjectionToken,
  provideBrowserGlobalErrorListeners,
  Provider,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideState, provideStore } from '@ngrx/store';
import { provideStoreDevtools } from '@ngrx/store-devtools';

import { apiBaseInterceptor, authInterceptor, metaHeadersInterceptor } from '@tchl/api';
import { environment } from '@tchl/config';
import { I18nEffects, i18nFeature } from '@tchl/data-access/i18n';
import { PageEffects, pageFeature } from '@tchl/data-access/page';
import { FEATURE_CONTEXT, FEATURE_INITIAL, provideFeatureClient } from '@tchl/feature';
import { AuthService, LocalStorageSecurityService } from '@tchl/shared/auth';
import { THEME_INIT_PROVIDER } from '@tchl/ui/theme';
import { I18nMergerService } from '@tchl/utils/i18n';
import { provideBuiltinWidgets } from '@tchl/web/widgets';

import {
  MergedTranslateLoader,
  MergedTranslateLoaderOptions,
} from '../../../../libs/shared/utils/i18n/src/lib/loader/merged-translate-loader';

import { appRoutes } from './app.routes';

export const TRANSLATE_LOADER_OPTIONS = new InjectionToken<MergedTranslateLoaderOptions>(
  'TRANSLATE_LOADER_OPTIONS',
);

export function initAuth(auth: AuthService) {
  return () => auth.init(); // ← restaure la session si possible, sans déclencher de login
}

export const authInitializer: Provider = {
  provide: APP_INITIALIZER,
  useFactory: initAuth,
  deps: [AuthService],
  multi: true,
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    // 1. Fournir le routeur de l'application
    provideRouter(appRoutes),

    // 2. Fournir le HttpClient, essentiel pour que les Effects puissent faire des appels API
    provideHttpClient(
      withFetch(),
      withInterceptors([apiBaseInterceptor, authInterceptor, metaHeadersInterceptor]),
    ),

    // 3. Fournir le Store NgRx (avec un état racine vide au début)
    provideStore(),

    // 4. Enregistrer notre "feature state" pour la page
    provideState(pageFeature),
    provideState(i18nFeature),

    // 5. Enregistrer nos "effects"
    provideEffects([PageEffects, I18nEffects]),

    // 6. (Optionnel mais fortement recommandé) Activer les DevTools pour le debug
    provideStoreDevtools({
      maxAge: 25, // Conserve les 25 derniers états
      logOnly: false, // Mettre à true en production
    }),

    {
      provide: TRANSLATE_LOADER_OPTIONS,
      useValue: {
        assetsPrefix: '/assets/i18n/',
        backendPath: '/v1/configs/i18n',
      },
    },
    provideTranslateService({
      loader: {
        provide: TranslateLoader,
        useFactory: (
          http: HttpClient,
          i18nMergerService: I18nMergerService,
          opts: MergedTranslateLoaderOptions,
        ) => new MergedTranslateLoader(http, i18nMergerService, opts),
        deps: [HttpClient, I18nMergerService, TRANSLATE_LOADER_OPTIONS],
      },
      fallbackLang: 'fr',
      lang: 'fr',
    }),

    provideAuth({
      config: {
        authority: environment.authUrl,
        redirectUrl: environment.appUrl + '/auth/callback',
        postLogoutRedirectUri: environment.appUrl,
        clientId: environment.authClientId,
        scope: 'openid profile email',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        renewTimeBeforeTokenExpiresInSeconds: 30,
        logLevel: LogLevel.Debug,
      },
    }),
    { provide: AbstractSecurityStorage, useClass: LocalStorageSecurityService },
    provideBuiltinWidgets(),
    authInitializer,
    THEME_INIT_PROVIDER,

    provideMarkdown(),
    //features
    { provide: FEATURE_INITIAL, useValue: [] },

    // Contexte d’évaluation (tenant, user, pays…)
    { provide: FEATURE_CONTEXT, useValue: { appName: 'tchalanet-web', tenantId: 'default' } },

    provideFeatureClient({
      kind: 'unleash',
      url: environment.feature.url,
      clientKey: environment.feature.clientKey,
      appName: environment.feature.appName,
      refreshInterval: environment.feature.refresh,
    }),
  ],
};
