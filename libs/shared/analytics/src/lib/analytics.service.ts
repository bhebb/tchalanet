import { Inject, Injectable } from '@angular/core';

import { ANALYTICS_CONFIG } from './analytics.tokens';
import { AnalyticsConfig, AnalyticsEvent } from './analytics.types';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(@Inject(ANALYTICS_CONFIG) private cfg: AnalyticsConfig) {}

  private logDebug(...args: unknown[]) {
    if (this.cfg.debug) console.debug('[analytics]', ...args);
  }

  pageView(url = location.pathname) {
    this.logDebug('pageView', url);
    try {
      if (this.cfg.provider === 'ga') {
        // gtag expects: gtag('event', 'page_view', { page_path: url })
        const gtag = (globalThis as any).gtag;
        if (typeof gtag === 'function') {
          gtag('event', 'page_view', { page_path: url });
          return;
        }
      }
    } catch (e) {
      // noop
    }
  }

  event(ev: AnalyticsEvent) {
    this.logDebug('event', ev);
    try {
      if (this.cfg.provider === 'ga') {
        const gtag = (globalThis as any).gtag;
        if (typeof gtag === 'function') {
          gtag('event', ev.name, ev.data || {});
          return;
        }
      }
    } catch (e) {
      // noop
    }
    // fallback dev
    if (this.cfg.provider === 'console') {
      console.info('[analytics:event]', ev.name, ev.data || {});
    }
  }

  // Helpers
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
