import { APP_INITIALIZER, FactoryProvider } from '@angular/core';

import { ThemeRepository } from './theme.repository';

export function themeInitFactory(repo: ThemeRepository) {
  return () => {
    repo.list();
  };
}

export const THEME_INIT_PROVIDER: FactoryProvider = {
  provide: APP_INITIALIZER,
  useFactory: themeInitFactory,
  deps: [ThemeRepository],
  multi: true,
};
