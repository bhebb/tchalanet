import { APP_INITIALIZER, FactoryProvider } from '@angular/core';
import { ThemeRepository } from './theme.repository';
import { ThemeAssetsLoader } from './theme-assets.loader';

export function themeInitFactory(loader: ThemeAssetsLoader, repo: ThemeRepository) {
  return async () => {
    const ids = await loader.listIds();
    const themes = await Promise.all(ids?.themes.map(theme => loader.fetch(theme.url)));
    repo.registerMany(themes);
  };
}

export const THEME_INIT_PROVIDER: FactoryProvider = {
  provide: APP_INITIALIZER,
  useFactory: themeInitFactory,
  deps: [ThemeAssetsLoader, ThemeRepository],
  multi: true,
};
