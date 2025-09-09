import { APP_INITIALIZER, Provider } from '@angular/core';
import { ANALYTICS_CONFIG } from './analytics.tokens';
import { AnalyticsConfig } from './analytics.types';

function injectUmami(cfg: AnalyticsConfig) {
  if (cfg.provider !== 'umami' || !cfg.umami) return;
  const { host, websiteId, autoTrack = false } = cfg.umami;
  const src = new URL('/script.js', host).toString();

  return () =>
    new Promise<void>(resolve => {
      // déjà présent ?
      if (document.querySelector(`script[data-website-id="${websiteId}"]`)) return resolve();
      const s = document.createElement('script');
      s.async = true;
      s.defer = true;
      s.dataset['websiteId'] = websiteId;
      s.src = src;
      if (!autoTrack) s.dataset['autoTrack'] = 'false';
      s.onload = () => resolve();
      s.onerror = () => resolve();
      document.head.appendChild(s);
    });
}

export function provideAnalyticsInit(): Provider {
  return {
    provide: APP_INITIALIZER,
    multi: true,
    deps: [ANALYTICS_CONFIG],
    useFactory: injectUmami,
  };
}
