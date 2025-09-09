import {
  APP_INITIALIZER,
  ApplicationConfig,
  InjectionToken,
  provideBrowserGlobalErrorListeners,
  Provider,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { HttpClient, provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import {
  MergedTranslateLoader,
  MergedTranslateLoaderOptions,
} from '../../../../libs/shared/utils/i18n/src/lib/loader/merged-translate-loader';
import { I18nMergerService } from '@tchl/utils/i18n';
import { apiBaseInterceptor, authInterceptor, metaHeadersInterceptor } from '@tchl/api';
import { provideState, provideStore } from '@ngrx/store';
import { PageEffects, pageFeature } from '@tchl/data-access/page';
import { I18nEffects, i18nFeature } from '@tchl/data-access/i18n';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { AbstractSecurityStorage, LogLevel, provideAuth } from 'angular-auth-oidc-client';
import { AuthService, LocalStorageSecurityService } from '@tchl/shared/auth';
import { provideBuiltinWidgets } from '@tchl/web/widgets';
import { THEME_INIT_PROVIDER } from '@tchl/ui/theme';
import {
  InstantSearchClient,
  MEILISEARCH_CONFIG,
} from '../../../../libs/shared/api/src/lib/client/instant-search-client';
import { MeilisearchConfig } from '@meilisearch/instant-meilisearch';
import { SearchIndexInitializerService } from '../../../../libs/web/widgets/src/lib/search-results/search-index-initializer.service';
import { ANALYTICS_CONFIG, provideAnalyticsInit, setupRouterPageViews } from '@tchl/analytics';
import { environment } from '@tchl/config';

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

// Fonction de création du client
export function createInstantSearchClient(config: MeilisearchConfig): InstantSearchClient {
  return new InstantSearchClient(config);
}

// Provider pour le client
export const INSTANT_SEARCH_CLIENT_PROVIDER: Provider = {
  provide: InstantSearchClient,
  useFactory: createInstantSearchClient,
  deps: [MEILISEARCH_CONFIG]
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
        authority: 'http://localhost:8080/realms/tchalanet',
        redirectUrl: 'http://localhost:4200/auth/callback',
        postLogoutRedirectUri: 'http://localhost:4200',
        clientId: 'tchalanet-web',
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

    {
      provide: ANALYTICS_CONFIG,
      useValue: {
        provider: 'umami',                  // 'console' en dev si tu veux
        umami: {
          host: environment.umami.host,    // ton instance Umami
          websiteId: environment.umami.websiteId,
          autoTrack: false,
        },
        debug: true,                        // logs console utiles
      }
    },
    provideAnalyticsInit(),
    // Router → pageviews
    { provide: 'ANALYTICS_ROUTER_BOOT', useFactory: setupRouterPageViews },
    {
      provide: MEILISEARCH_CONFIG,
      useValue: {
        host: 'http://localhost:7700',
        apiKey: 'dev-meili'
      }
    },
    INSTANT_SEARCH_CLIENT_PROVIDER,
    {
      provide: APP_INITIALIZER,
      useFactory: (indexInitializer: SearchIndexInitializerService) =>
        () => indexInitializer.initializeIndexes(),
      deps: [SearchIndexInitializerService],
      multi: true
    }


  ],
};
