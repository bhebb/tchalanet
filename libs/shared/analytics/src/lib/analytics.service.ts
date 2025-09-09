import { Inject, Injectable } from '@angular/core';
import { ANALYTICS_CONFIG } from './analytics.tokens';
import { AnalyticsConfig, AnalyticsEvent } from './analytics.types';

declare global {
  interface Window {
    umami?: {
      track: (event: string, data?: Record<string, unknown>) => void;
      trackView?: (url?: string, referrer?: string) => void;
    };
  }
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(@Inject(ANALYTICS_CONFIG) private cfg: AnalyticsConfig) {}

  private logDebug(...args: unknown[]) {
    if (this.cfg.debug) console.debug('[analytics]', ...args);
  }

  pageView(url = location.pathname) {
    this.logDebug('pageView', url);
    if (this.cfg.provider === 'umami' && window.umami?.trackView) {
      window.umami.trackView(url, document.referrer || undefined);
    }
  }

  event(ev: AnalyticsEvent) {
    this.logDebug('event', ev);
    if (this.cfg.provider === 'umami' && window.umami?.track) {
      window.umami.track(ev.name, ev.data);
      return;
    }
    // fallback dev
    if (this.cfg.provider === 'console') {
      console.info('[analytics:event]', ev.name, ev.data || {});
    }
  }

  // Helpers “prêts à l’emploi”
  searchViewSuggestions(q: string, meta?: Record<string, unknown>) {
    this.event({ name: 'view_suggestions', data: { q, ...meta } });
  }
  searchNoResults(q: string, meta?: Record<string, unknown>) {
    this.event({ name: 'search_no_results', data: { q, ...meta } });
  }
  selectSearchResult(id: string, position: number, meta?: Record<string, unknown>) {
    this.event({ name: 'select_search_result', data: { id, position, ...meta } });
  }
}
