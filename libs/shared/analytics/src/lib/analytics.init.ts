import { APP_INITIALIZER, Provider } from '@angular/core';
import { ANALYTICS_CONFIG } from './analytics.tokens';
import { AnalyticsConfig } from './analytics.types';

// No-op initializer: analytics script is loaded from index.html (gtag) or handled by provider.
function noopInit(_cfg: AnalyticsConfig) {
  return () => () => Promise.resolve();
}

export function provideAnalyticsInit(): Provider {
  return {
    provide: APP_INITIALIZER,
    multi: true,
    deps: [ANALYTICS_CONFIG],
    useFactory: noopInit,
  };
}
