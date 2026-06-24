import { Route } from '@angular/router';

export const platformCatalogRoutes: Route[] = [
  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/platform-catalog-settings.page').then(
        m => m.PlatformCatalogSettingsPage,
      ),
  },
  {
    path: 'themes',
    loadComponent: () =>
      import('./pages/themes/platform-catalog-themes.page').then(
        m => m.PlatformCatalogThemesPage,
      ),
  },
  {
    path: 'games',
    loadComponent: () =>
      import('./pages/games/platform-catalog-games.page').then(
        m => m.PlatformCatalogGamesPage,
      ),
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/platform-catalog-pricing.page').then(
        m => m.PlatformCatalogPricingPage,
      ),
  },
  {
    path: 'draw-channels',
    loadComponent: () =>
      import('./pages/draw-channels/platform-catalog-draw-channels.page').then(
        m => m.PlatformCatalogDrawChannelsPage,
      ),
  },
  {
    path: 'result-slots',
    loadComponent: () =>
      import('./pages/result-slots/platform-catalog-result-slots.page').then(
        m => m.PlatformCatalogResultSlotsPage,
      ),
  },
  {
    path: 'plans-pricing',
    loadComponent: () =>
      import('./pages/plans/platform-catalog-plans.page').then(
        m => m.PlatformCatalogPlansPage,
      ),
  },
  {
    path: 'translations',
    loadComponent: () =>
      import('./pages/translations/platform-catalog-i18n-overrides.page').then(
        m => m.PlatformCatalogI18nOverridesPage,
      ),
  },
  {
    path: 'page-model-templates',
    loadComponent: () =>
      import('./pages/page-model-templates/platform-catalog-page-model-templates.page').then(
        m => m.PlatformCatalogPageModelTemplatesPage,
      ),
  },
];
